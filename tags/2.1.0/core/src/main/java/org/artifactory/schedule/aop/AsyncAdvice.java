/*
 * This file is part of Artifactory.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.tx.SessionResource;
import org.slf4j.Logger;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
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
                        "Async invocation scheduled for until commit, cannot be scheduled outside a transaction.");
            } else {
                log.debug("Async invocation scheduled for until commit, but not scheduled inside a transaction.");
            }
        }
        if (delayExecutionUntilCommit && inTransaction) {
            //Schedule task submission for session save()
            InternalArtifactoryContext context = InternalContextHelper.get();
            JcrService jcrService = context.getJcrService();
            //Mark the thread as async so that we don't open tx that are part this session, but another session after comtmit
            JcrSession session = jcrService.getManagedSession();
            MethodCallbackSessionResource sessionCallbacks =
                    session.getOrCreateResource(MethodCallbackSessionResource.class);
            sessionCallbacks.addInvocation(invocation);
            //No future
            return null;
        } else {
            //Submit immediately
            Future<Object> future = submit(invocation);
            return future;
        }
    }

    private static Future<Object> submit(final MethodInvocation invocation) {
        final InternalArtifactoryContext context = InternalContextHelper.get();
        final ArtifactorySystemProperties artifactorySystemProperties = ArtifactorySystemProperties.get();
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //Use the same executor pool used by quartz
        CachedThreadPoolTaskExecutor executor = context.beanForType(CachedThreadPoolTaskExecutor.class);
        ExecutorService executorService = executor.getConcurrentExecutor();
        final Object[] result = new Object[1];
        Future<Object> future = executorService.submit(new Runnable() {
            public void run() {
                //Rebind the context and security
                ArtifactorySystemProperties.bind(artifactorySystemProperties);
                ArtifactoryContextThreadBinder.bind(context);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                try {
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        //Sanity check we should never have a tx sync on an existing pooled thread
                        throw new IllegalStateException(
                                "An async invocation (" + invocation.getMethod() +
                                        ") should not be associated with an existing transaction.");
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
                } catch (Throwable throwable) {
                    log.error("Could not execute async method: '" + invocation.getMethod() + "'.", throwable);
                } finally {
                    SecurityContextHolder.setContext(null);
                    ArtifactoryContextThreadBinder.unbind();
                    ArtifactorySystemProperties.unbind();
                }
            }
        }, result[0]);
        return future;
    }

    public static class MethodCallbackSessionResource implements SessionResource {

        List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();

        public void addInvocation(MethodInvocation invocation) {
            invocations.add(invocation);
        }

        public void afterCompletion(boolean commit) {
            if (commit) {
                if (!invocations.isEmpty()) {
                    //Clear the invocations for this session and submit them for async execution
                    List<MethodInvocation> tmpInvocations = new ArrayList<MethodInvocation>();
                    tmpInvocations.addAll(invocations);
                    //Reset internal state
                    invocations.clear();
                    for (MethodInvocation invocation : tmpInvocations) {
                        submit(invocation);
                    }
                }
            } else if (!invocations.isEmpty()) {
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