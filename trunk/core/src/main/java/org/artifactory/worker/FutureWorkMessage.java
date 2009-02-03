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
package org.artifactory.worker;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author freds
 * @date Oct 30, 2008
 */
public abstract class FutureWorkMessage<V> extends WorkMessage implements Future<V> {
    private final FutureTask<V> task;
    private final Callable<V> clientCallback;
    private RepoServiceCallable<V> repoServiceCallable;

    class RepoServiceCallable<V> implements Callable<V> {
        private V result;
        private Throwable exception;

        public V call() throws Exception {
            InternalRepositoryService service =
                    InternalContextHelper.get().beanForType(InternalRepositoryService.class);
            service.executeMessage(FutureWorkMessage.this);
            return result;
        }

        public void setResult(V result) {
            this.result = result;
        }

        public void setException(Throwable exception) {
            this.exception = exception;
        }

        public Throwable getException() {
            return exception;
        }
    }

    protected FutureWorkMessage(WorkAction action,
            RepoPath repoPath, Callable<V> callback) {
        super(action, repoPath);
        clientCallback = callback;
        repoServiceCallable = new RepoServiceCallable<V>();
        task = new FutureTask<V>(repoServiceCallable);
    }

    @Override
    protected void call() {
        task.run();
    }

    public final void execute() {
        try {
            repoServiceCallable.setResult(clientCallback.call());
        } catch (Exception e) {
            repoServiceCallable.setException(e);
        }
    }

    public boolean isCancelled() {
        return task.isCancelled();
    }

    public boolean isDone() {
        return task.isDone();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return task.cancel(mayInterruptIfRunning);
    }

    public V get() throws InterruptedException, ExecutionException {
        return task.get();
    }

    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return task.get(timeout, unit);
    }
}
