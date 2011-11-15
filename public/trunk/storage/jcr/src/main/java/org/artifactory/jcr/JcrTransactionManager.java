/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.jcr;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.util.db.ArtifactoryConnectionHelper;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.LoggingUtils;
import org.slf4j.Logger;
import org.springframework.extensions.jcr.jackrabbit.LocalTransactionManager;
import org.springframework.extensions.jcr.jackrabbit.support.UserTxSessionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author yoavl
 */
@Component("transactionManager")
public class JcrTransactionManager extends LocalTransactionManager implements TransactionSynchronization {
    private static final Logger log = LoggerFactory.getLogger(JcrTransactionManager.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        //Avoid the check on null repo will be initialized by Jcr class
    }

    @Override
    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        super.prepareSynchronization(status, definition);
        if (status.isNewSynchronization()) {
            TransactionSynchronizationManager.registerSynchronization(this);
        }
    }

    @Override
    protected void prepareForCommit(DefaultTransactionStatus status) {
        super.prepareForCommit(status);
        if (!status.isRollbackOnly()) {
            JcrSession session = getCurrentSession();
            if (status.isReadOnly() && session.isLive() && session.hasPendingChanges()) {
                status.setRollbackOnly();
                session.refresh(false);
                LoggingUtils.warnOrDebug(log, "Discarding changes made by a read-only transaction.");
            }
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        //Print debug info
        traceTx(status, false);
        //Now send the commit
        try {
            super.doCommit(status);
        } catch (RuntimeException e) {
            String msg = "Could not commit transaction: " + e.getMessage();
            StaleItemStateException staleItem =
                    (StaleItemStateException) ExceptionUtils.getCauseOfTypes(e, StaleItemStateException.class);
            if (staleItem != null) {
                String parentUuid = staleItem.getParentUuid();
                if (parentUuid != null) {
                    msg += "\nStale node path: '" + JcrUtils.nodePathFromUuid(parentUuid) + "'";
                }
            }
            log.error(msg);
            traceTx(status, true);
            throw e;
        }
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        super.doSetRollbackOnly(status);
        //Discard any pending changes so that we will not find them in catch clauses for example
        //(see: RTFACT-547)
        JcrSession session = getCurrentSession();
        // Release early
        session.getSessionResourceManager().afterCompletion(false);
        if (session.isLive() && session.hasPendingChanges()) {
            if (log.isDebugEnabled()) {
                log.debug("Early changes discarding for a rolled back transaction.");
                session.refresh(false);
            }
        }
    }

    public void suspend() {
    }

    public void resume() {
    }

    public void beforeCommit(boolean readOnly) {
        //Save any pending changes (no need to test for rollback at this phase)
        JcrSession session = getCurrentSession();
        if (log.isDebugEnabled()) {
            log.debug("Saving session: " + session + ".");
        }
        if (!readOnly && session.isLive()) {
            //Flush the changes - as early as possible to save on memory resources + fires up save in session resources
            session.save();
        }
    }

    public void beforeCompletion() {
    }

    public void afterCommit() {
    }

    public void flush() {
    }


    @Override
    protected void doBegin(Object transaction, TransactionDefinition transactionDefinition)
            throws TransactionException {
        super.doBegin(transaction, transactionDefinition);
        JcrSession session = getCurrentSession();
        ArtifactoryConnectionHelper connectionHelper = JcrUtils.getExtendedDataStore(session).getConnectionHelper();
        if (!connectionHelper.isTxActive()) {
            connectionHelper.txBegin();
        }
    }

    public void afterCompletion(int status) {
        boolean success = status == TransactionSynchronization.STATUS_COMMITTED;
        JcrSession session = getCurrentSession();
        //Close the transaction and commit the datastore connetion helper batch (same instance used by cspaths)
        ArtifactoryConnectionHelper connectionHelper = JcrUtils.getExtendedDataStore(session).getConnectionHelper();
        connectionHelper.txEnd(success);
        // Commit the locks/discard changes on rollback
        session.getSessionResourceManager().afterCompletion(success);
        if (!success) {
            if (session.isLive()) {
                session.refresh(false);
            } else {
                log.debug("Cannot refresh session - session is no longer alive.");
            }
        }
    }

    private void traceTx(DefaultTransactionStatus status, boolean force) {
        if (log.isTraceEnabled() || force) {
            String msg = "status = " + status;
            log(msg, force);
            if (status != null) {
                Object txobj = status.getTransaction();
                msg = "txobj = " + txobj;
                log(msg, force);
                try {
                    if (txobj != null) {
                        Object sh = MethodUtils.invokeMethod(txobj, "getSessionHolder", null);
                        msg = "sh = " + sh;
                        log(msg, force);
                        if (sh != null) {
                            Object tx = MethodUtils.invokeMethod(sh, "getTransaction", null);
                            msg = "tx = " + tx;
                            log(msg, force);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private JcrSession getCurrentSession() {
        UserTxSessionHolder sessionHolder =
                (UserTxSessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
        JcrSession session = (JcrSession) sessionHolder.getSession();
        return session;
    }

    private void log(String msg, boolean info) {
        if (info) {
            log.info(msg);
        } else {
            log.trace(msg);
        }
    }
}