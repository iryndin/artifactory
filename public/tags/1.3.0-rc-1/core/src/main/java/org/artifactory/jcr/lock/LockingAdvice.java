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
package org.artifactory.jcr.lock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.artifactory.api.repo.Lock;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author freds
 * @date Oct 27, 2008
 */
public class LockingAdvice implements MethodInterceptor {
    private static final Logger log =
            LoggerFactory.getLogger(LockingAdvice.class);

    private static final ThreadLocal<InternalLockManager> lockHolder = new ThreadLocal<InternalLockManager>();
    private MethodInterceptor alwaysOnTxInterceptor;

    public LockingAdvice() {
        log.info("Creating locking Advice interceptor");
    }

    public MethodInterceptor getAlwaysOnTxInterceptor() {
        if (alwaysOnTxInterceptor == null) {
            BeanFactory beanFactory = InternalContextHelper.get();
            alwaysOnTxInterceptor =
                    (MethodInterceptor) beanFactory.getBean("alwaysOnTxInterceptor");
            log.info("Locking interceptor has Tx advice {}", alwaysOnTxInterceptor);
        }
        return alwaysOnTxInterceptor;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        boolean initTx = false;
        Lock lockAnnotation = invocation.getMethod().getAnnotation(Lock.class);
        if (lockAnnotation == null) {
            log.error("Method " + invocation.getMethod() + " does not have the annotation " +
                    Lock.class.getName() + " inside the LockingAdvice interceptor");
        } else {
            initTx = lockAnnotation.transactional();
        }
        // if already inside a transaction, do not add one (no Tx interceptor nesting)
        if (initTx && LockingHelper.isInJcrTransaction()) {
            initTx = false;
        }
        InternalLockManager currentLockManager;
        InternalLockManager previousLockManager = getLockManager();
        if (previousLockManager == null) {
            currentLockManager = createLockManager();
        } else {
            currentLockManager = previousLockManager;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Activating Locking Interceptor on " + invocation.getMethod() +
                        " with tx " +
                        initTx + " and previous lm " + previousLockManager);
            }
            if (initTx) {
                return getAlwaysOnTxInterceptor().invoke(invocation);
            } else {
                return invocation.proceed();
            }
        } finally {
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

    private static InternalLockManager createLockManager() {
        if (LockingHelper.isInJcrTransaction()) {
            throw new IllegalStateException("Cannot create Session lock inside a JCR Transaction!\n" +
                    "Please use @Lock(transactional = true) instead of @Transactional");
        }
        InternalLockManager result = new InternalLockManager();
        lockHolder.set(result);
        return result;
    }

    public static void clearLockManager() {
        lockHolder.remove();
    }
}
