/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr;

import org.artifactory.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springmodules.jcr.jackrabbit.LocalTransactionManager;
import org.springmodules.jcr.jackrabbit.support.UserTxSessionHolder;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Component("transactionManager")
public class JcrTransactionManager extends LocalTransactionManager
        implements TransactionSynchronization {
    private static final Logger log = LoggerFactory.getLogger(JcrTransactionManager.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        //Avoid the check on null repo will be initialized by Jcr class
    }

    @Override
    protected DefaultTransactionStatus newTransactionStatus(TransactionDefinition definition,
            Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug,
            Object suspendedResources) {
        DefaultTransactionStatus status =
                super.newTransactionStatus(definition, transaction, newTransaction,
                        newSynchronization, debug, suspendedResources);
        //Register ourselves to get transaction sync events
        TransactionSynchronizationManager.registerSynchronization(this);
        return status;
    }

    @Override
    protected void prepareForCommit(DefaultTransactionStatus status) {
        super.prepareForCommit(status);
        if (!status.isRollbackOnly()) {
            UserTxSessionHolder sessionHolder =
                    (UserTxSessionHolder) TransactionSynchronizationManager
                            .getResource(getSessionFactory());
            JcrSession session = (JcrSession) sessionHolder.getSession();
            if (status.isReadOnly() && session.isLive() && session.hasPendingChanges()) {
                status.setRollbackOnly();
                session.refresh(false);
                LoggingUtils.warnOrDebug(
                        log, "Discarding changes made by a read-only transaction.");
            }
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        //Save any pending changes (no need to test for rollback at this phase)
        UserTxSessionHolder sessionHolder =
                (UserTxSessionHolder) TransactionSynchronizationManager
                        .getResource(getSessionFactory());
        JcrSession session = (JcrSession) sessionHolder.getSession();
        if (log.isDebugEnabled()) {
            log.debug("Saving session: " + session + ".");
        }
        if (!status.isRollbackOnly() && !status.isReadOnly() && session.isLive()) {
            //Flush the changes as early as possible to save on memory resources
            session.save();
        }
        //Now send the commit
        super.doCommit(status);
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        super.doSetRollbackOnly(status);
        //Discard any pending changes so that we will not find them in catch clauses for example
        //(see: RTFACT-547)
        UserTxSessionHolder sessionHolder =
                (UserTxSessionHolder) TransactionSynchronizationManager
                        .getResource(getSessionFactory());
        JcrSession session = (JcrSession) sessionHolder.getSession();
        // Release early
        session.getSessionResources().releaseResources(false);
        if (session.isLive() && session.hasPendingChanges()) {
            if (log.isDebugEnabled()) {
                log.debug("Early changes discrading for a rolled back transaction.");
                session.refresh(false);
            }
        }
    }

    public void suspend() {
    }

    public void resume() {
    }

    public void beforeCommit(boolean readOnly) {
    }

    public void beforeCompletion() {
    }

    public void afterCommit() {
    }

    public void afterCompletion(int status) {
        UserTxSessionHolder sessionHolder =
                (UserTxSessionHolder) TransactionSynchronizationManager
                        .getResource(getSessionFactory());
        JcrSession session = (JcrSession) sessionHolder.getSession();
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            // Commit the locks
            session.getSessionResources().releaseResources(true);
        } else {
            //Discard changes on rollback
            session.getSessionResources().releaseResources(false);
            session.refresh(false);
        }
    }
}