/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.schedule.aop;

import com.google.common.collect.Lists;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.tx.SessionResource;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Yoav Landman
 */
public class AsyncAdvice implements MethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AsyncAdvice.class);

    public AsyncAdvice() {
        log.debug("Creating async advice interceptor");
    }

    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (method.getAnnotation(Lock.class) != null) {
            throw new RuntimeException("The @Async annotation cannot be used with the @Lock annotation. " +
                    "Use @Async#transactional=true instead: " + invocation.getMethod());
        }
        Async annotation = method.getAnnotation(Async.class);
        boolean delayExecutionUntilCommit = annotation.delayUntilAfterCommit();
        boolean failIfNotScheduledFromTransaction = annotation.failIfNotScheduledFromTransaction();
        boolean inTransaction = LockingAdvice.isInJcrTransaction();
        if (!inTransaction && delayExecutionUntilCommit) {
            if (failIfNotScheduledFromTransaction) {
                throw new IllegalStateException(
                        "Async invocation scheduled for after commit, cannot be scheduled outside a transaction.");
            } else {
                log.debug("Async invocation scheduled for after commit, but not scheduled inside a transaction.");
            }
        }

        if (delayExecutionUntilCommit && inTransaction) {
            boolean shared = annotation.shared();
            //Schedule task submission for session save()
            InternalArtifactoryContext context = InternalContextHelper.get();
            JcrService jcrService = context.getJcrService();
            //Mark the thread as async so that we don't open tx that are part this session, but another session after
            //commit
            JcrSession session = jcrService.getManagedSession();
            MethodCallbackSessionResource sessionCallbacks =
                    session.getOrCreateResource(MethodCallbackSessionResource.class);
            sessionCallbacks.addInvocation(invocation, shared);
            //No future
            return null;
        } else {
            //Submit immediately
            Future<?> future = submit(invocation);
            return future;
        }
    }

    private static Future<?> submit(final MethodInvocation invocation) {
        final InternalArtifactoryContext context = InternalContextHelper.get();
        CachedThreadPoolTaskExecutor executor = context.beanForType(CachedThreadPoolTaskExecutor.class);
        final Object[] result = new Object[1];
        Future<?> future = executor.submit(new Runnable() {
            public void run() {
                try {
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        //Sanity check we should never have a tx sync on an existing pooled thread
                        throw new IllegalStateException(
                                "An async invocation (" + invocation.getMethod() +
                                        ") should not be associated with an existing transaction.");
                    }
                    invoke(invocation, result);
                } catch (Throwable throwable) {
                    log.error("Could not execute async method: '" + invocation.getMethod() + "'.", throwable);
                }
            }
        }, result[0]);
        return future;
    }

    private static void invoke(MethodInvocation invocation, Object[] result) throws Throwable {
        if (invocation instanceof CompoundInvocation) {
            invocation.proceed();
            return;
        }
        Async annotation = invocation.getMethod().getAnnotation(Async.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "An async invocation (" + invocation.getMethod() +
                            ") should be used with an @Async annotated invocation.");
        }
        if (annotation.transactional()) {
            //Wrap in a transaction
            result[0] = new LockingAdvice().invoke(invocation, true);
        } else {
            result[0] = invocation.proceed();
        }
    }

    public static class MethodCallbackSessionResource implements SessionResource {

        final List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
        final CompoundInvocation sharedInvocations = new CompoundInvocation();

        public void addInvocation(MethodInvocation invocation, boolean shared) {
            if (shared) {
                sharedInvocations.add(invocation);
            } else {
                invocations.add(invocation);
            }
        }

        public void afterCompletion(boolean commit) {
            if (commit) {
                //Submit the shared ones first
                if (!sharedInvocations.isEmpty()) {
                    submit(sharedInvocations);
                }
                if (!invocations.isEmpty()) {
                    //Clear the invocations for this session and submit them for async execution
                    ArrayList<MethodInvocation> tmpInvocations = Lists.newArrayList(invocations);
                    //Reset internal state
                    invocations.clear();
                    for (MethodInvocation invocation : tmpInvocations) {
                        submit(invocation);
                    }
                }
            } else {
                sharedInvocations.clear();
                invocations.clear();
            }
        }

        public boolean hasPendingResources() {
            return !invocations.isEmpty();
        }

        public void onSessionSave() {
        }
    }
}