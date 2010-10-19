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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author Yoav Landman
 */
public class AsyncAdvice implements MethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AsyncAdvice.class);

    private static Set<MethodInvocation> pendingInvocations = Collections.synchronizedSet(
            new HashSet<MethodInvocation>());

    public AsyncAdvice() {
        log.debug("Creating async advice interceptor");
    }

    public Future<?> invoke(final MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (method.getAnnotation(Lock.class) != null) {
            throw new RuntimeException("The @Async annotation cannot be used with the @Lock annotation. " +
                    "Use @Async#transactional=true instead: " + method);
        }
        Async annotation = method.getAnnotation(Async.class);
        boolean delayExecutionUntilCommit = annotation.delayUntilAfterCommit();
        boolean failIfNotScheduledFromTransaction = annotation.failIfNotScheduledFromTransaction();
        boolean inTransaction = LockingAdvice.isInJcrTransaction();
        if (!inTransaction && delayExecutionUntilCommit) {
            if (failIfNotScheduledFromTransaction) {
                throw new IllegalStateException("Async invocation scheduled for after commit, " +
                        "cannot be scheduled outside a transaction: " + method);
            } else {
                log.debug("Async invocation scheduled for after commit, but not scheduled inside a transaction: {}",
                        method);
            }
        }

        //noinspection ThrowableInstanceNeverThrown
        TraceableMethodInvocation traceableInvocation =
                new TraceableMethodInvocation(invocation, Thread.currentThread().getName());
        log.trace("Adding: {}", traceableInvocation);
        pendingInvocations.add(traceableInvocation);
        try {
            if (delayExecutionUntilCommit && inTransaction) {
                //Schedule task submission for session save()
                JcrService jcrService = InternalContextHelper.get().getJcrService();
                JcrSession session = jcrService.getManagedSession();
                MethodCallbackSessionResource sessionCallbacks =
                        session.getOrCreateResource(MethodCallbackSessionResource.class);
                sessionCallbacks.setAdvice(this);
                sessionCallbacks.addInvocation(traceableInvocation, annotation.shared());
                //No future
                return null;
            } else {
                //Submit immediately
                Future<?> future = submit(traceableInvocation);
                return future;
            }
        } catch (Exception e) {
            // making sure to remove the invocation from the pending/executing
            removeInvocation(traceableInvocation);
            throw e;
        }
    }

    private Future<?> submit(final MethodInvocation invocation) {
        InternalArtifactoryContext context = InternalContextHelper.get();
        CachedThreadPoolTaskExecutor executor = context.beanForType(CachedThreadPoolTaskExecutor.class);
        Future<?> future = executor.submit(new Callable<Object>() {
            public Object call() {
                try {
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        //Sanity check we should never have a tx sync on an existing pooled thread
                        throw new IllegalStateException(
                                "An async invocation (" + invocation.getMethod() + ") " +
                                        "should not be associated with an existing transaction.");
                    }
                    Object result = doInvoke(invocation);
                    // if the result is not of type Future don't bother returning it (unless you are fond of ClassCastExceptions)
                    if (result instanceof Future) {
                        return ((Future) result).get();
                    } else {
                        return null;
                    }
                } catch (Throwable throwable) {
                    Throwable loggedThrowable;
                    if (invocation instanceof TraceableMethodInvocation) {
                        Throwable original = ((TraceableMethodInvocation) invocation).getThrowable();
                        original.initCause(throwable);
                        loggedThrowable = original;
                    } else {
                        loggedThrowable = throwable;
                    }
                    Method method;
                    if (invocation instanceof CompoundInvocation) {
                        method = ((CompoundInvocation) invocation).getLatestMethod();
                    } else {
                        method = invocation.getMethod();
                    }
                    log.error("Could not execute async method: '" + method + "'.", loggedThrowable);
                    return null;
                }
            }
        });

        // only return the future result if the method returns a Future object
        if (!(invocation instanceof CompoundInvocation) &&
                Future.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
            return future;
        } else {
            return null;
        }
    }

    Object doInvoke(MethodInvocation invocation) throws Throwable {
        if (invocation instanceof CompoundInvocation) {
            invocation.proceed();
            return null;    // multiple invocations -> no single return type
        }
        try {
            Async annotation = invocation.getMethod().getAnnotation(Async.class);
            if (annotation == null) {
                throw new IllegalArgumentException(
                        "An async invocation (" + invocation.getMethod() +
                                ") should be used with an @Async annotated invocation.");
            }
            if (annotation.transactional()) {
                //Wrap in a transaction
                log.trace("Invoking {} in transaction", invocation);
                return new LockingAdvice().invoke(invocation, true);
            } else {
                log.trace("Invoking {} ", invocation);
                return invocation.proceed();
            }
        } finally {
            // remove the invocations here (called from the Compound also)
            removeInvocation(invocation);
        }
    }

    private void removeInvocation(MethodInvocation invocation) {
        log.trace("Removing: {}", invocation);
        pendingInvocations.remove(invocation);
    }

    public ImmutableSet<MethodInvocation> getCurrentInvocations() {
        return ImmutableSet.copyOf(pendingInvocations);
    }

    /**
     * @param method The method to check if pending execution (usually the interface method, not the implementation!)
     * @return True if there is an pending (or running) async call to the given method
     */
    public boolean isPending(Method method) {
        // iterate on a copy to avoid ConcurrentModificationException
        for (MethodInvocation invocation : getCurrentInvocations()) {
            if (invocation instanceof CompoundInvocation) {
                ImmutableList<MethodInvocation> invocations = ((CompoundInvocation) invocation).getInvocations();
                for (MethodInvocation methodInvocation : invocations) {
                    if (method.equals(methodInvocation.getMethod())) {
                        return true;
                    }
                }
            } else {
                if (method.equals(invocation.getMethod())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class MethodCallbackSessionResource implements SessionResource {
        AsyncAdvice advice;
        final List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
        final CompoundInvocation sharedInvocations = new CompoundInvocation();

        public void setAdvice(AsyncAdvice advice) {
            this.advice = advice;
            sharedInvocations.setAdvice(advice);
        }

        public void addInvocation(TraceableMethodInvocation invocation, boolean shared) {
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
                    advice.submit(sharedInvocations);
                }
                if (!invocations.isEmpty()) {
                    //Clear the invocations for this session and submit them for async execution
                    ArrayList<MethodInvocation> tmpInvocations = Lists.newArrayList(invocations);
                    //Reset internal state
                    invocations.clear();
                    for (MethodInvocation invocation : tmpInvocations) {
                        advice.submit(invocation);
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

    private static class TraceableMethodInvocation implements MethodInvocation {

        private final MethodInvocation wrapped;
        private final Throwable throwable;

        public TraceableMethodInvocation(MethodInvocation wrapped, String threadName) {
            this.wrapped = wrapped;
            String msg = "[" + threadName + "] async call to '" + wrapped.getMethod() + "' completed with error.";
            this.throwable = new Throwable(msg);
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public Method getMethod() {
            return wrapped.getMethod();
        }

        public Object[] getArguments() {
            return wrapped.getArguments();
        }

        public Object proceed() throws Throwable {
            return wrapped.proceed();
        }

        public Object getThis() {
            return wrapped.getThis();
        }

        public AccessibleObject getStaticPart() {
            return wrapped.getStaticPart();
        }

        @Override
        public String toString() {
            return wrapped.toString();
        }
    }
}