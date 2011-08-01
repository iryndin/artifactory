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

package org.artifactory.jcr.lock.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.lucene.util.CloseableThreadLocal;
import org.artifactory.api.repo.Lock;
import org.artifactory.jcr.lock.InternalLockManager;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author freds
 * @date Oct 27, 2008
 */
public class LockingAdvice implements MethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LockingAdvice.class);

    private static final ThreadLocal<InternalLockManager> lockHolder = new ThreadLocal<InternalLockManager>();
    private MethodInterceptor alwaysOnTxInterceptor;

    public LockingAdvice() {
        log.debug("Creating locking Advice interceptor");
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        return invoke(invocation, false);
    }

    public Object invoke(MethodInvocation invocation, boolean initTx) throws Throwable {
        if (!initTx) {
            //Check if the annotation mandates init tx
            Lock lockAnnotation = invocation.getMethod().getAnnotation(Lock.class);
            initTx = lockAnnotation.transactional();
        }
        //If already inside a transaction, do not add one (no Tx interceptor nesting)
        if (initTx && isInJcrTransaction()) {
            log.trace("Tx already active on {} - no need starting a new one.", invocation.getMethod());
            initTx = false;
        }
        InternalLockManager currentLockManager;
        //Reuse the existing lock manager and release the locks when the first lm returns
        InternalLockManager previousLockManager = getLockManager();
        if (previousLockManager == null) {
            currentLockManager = createLockManager();
        } else {
            currentLockManager = previousLockManager;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Activating Locking Interceptor on " + invocation.getMethod() +
                        " with tx " + initTx + " and previous lm " + previousLockManager);
            }
            if (initTx) {
                return getAlwaysOnTxInterceptor().invoke(invocation);
            } else {
                return invocation.proceed();
            }
        } finally {
            //When the first lm returns, clean up resources and remove the lm from the current thread
            if (previousLockManager == null) {
                try {
                    currentLockManager.releaseResources();
                } finally {
                    clearLockManager();
                }
            }
        }
    }

    public static InternalLockManager getLockManager() {
        return lockHolder.get();
    }

    public static void clearLockManager() {
        lockHolder.remove();
        // The lock manager removed no more JCR queries
        CloseableThreadLocal.closeAllThreadLocal();
    }

    public static boolean isInJcrTransaction() {
        return TransactionSynchronizationManager.isSynchronizationActive();
    }

    private MethodInterceptor getAlwaysOnTxInterceptor() {
        if (alwaysOnTxInterceptor == null) {
            BeanFactory beanFactory = InternalContextHelper.get();
            alwaysOnTxInterceptor = (MethodInterceptor) beanFactory.getBean("alwaysOnTxInterceptor");
            log.debug("Locking interceptor has Tx advice {}", alwaysOnTxInterceptor);
        }
        return alwaysOnTxInterceptor;
    }

    private static InternalLockManager createLockManager() {
        if (isInJcrTransaction()) {
            throw new IllegalStateException("Cannot create Session lock inside a JCR Transaction!\n" +
                    "Please use @Lock(transactional = true) instead of @Transactional");
        }
        InternalLockManager result = new InternalLockManager();
        lockHolder.set(result);
        return result;
    }
}
