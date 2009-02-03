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

import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.jetlang.channels.Channel;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Callback;
import org.jetlang.core.Disposable;
import org.jetlang.core.Filter;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author freds
 * @date Sep 18, 2008
 */
@Service
public class WorkerServiceImpl implements WorkerService {
    private final static Logger log = LoggerFactory.getLogger(WorkerServiceImpl.class);

    @Autowired
    private InternalRepositoryService repoService;

    private Map<RepositoryFiberKey, RepositoryChannel> channels;

    private ExecutorService executor;

    private PoolFiberFactory fiberFactory;

    private InternalArtifactoryContext context;

    @PostConstruct
    public void register() {
        context = InternalContextHelper.get();
        context.addReloadableBean(WorkerService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalRepositoryService.class};
    }

    public void init() {
        executor = Executors.newCachedThreadPool();
        fiberFactory = new PoolFiberFactory(executor);
        List<LocalRepoDescriptor> localRepoDescriptorList =
                repoService.getLocalAndCachedRepoDescriptors();
        //Create work message channels for each type of action per repo
        final WorkAction[] actions = WorkAction.values();
        channels = new HashMap<RepositoryFiberKey, RepositoryChannel>(
                localRepoDescriptorList.size() * actions.length);
        for (LocalRepoDescriptor localRepoDescriptor : localRepoDescriptorList) {
            String key = localRepoDescriptor.getKey();
            for (WorkAction action : actions) {
                RepositoryFiberKey filter = new RepositoryFiberKey(key, action);
                channels.put(filter, new RepositoryChannel(filter));
            }
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        destroy();
        init();
    }

    public void publish(WorkMessage msg) {
        final RepositoryFiberKey key = new RepositoryFiberKey(msg);
        final RepositoryChannel channel = channels.get(key);
        if (channel == null) {
            throw new RuntimeException(
                    "No Worker was initialized for key " + key + " and work message " + msg);
        }
        channel.publish(msg);
    }

    public void destroy() {
        if (channels != null) {
            for (RepositoryChannel repositoryChannel : channels.values()) {
                repositoryChannel.stopPublish();
            }
            for (RepositoryChannel repositoryChannel : channels.values()) {
                repositoryChannel.dispose();
            }
        }
        if (executor != null) {
            executor.shutdown();
        }
        if (fiberFactory != null) {
            fiberFactory.dispose();
        }
    }

    private Fiber createFiber() {
        return fiberFactory.create();
    }

    private class RepositoryChannel
            implements Disposable, Publisher<WorkMessage>, Callback<WorkMessage> {

        private final Fiber receiver;

        private final Channel<WorkMessage> channel;

        private final Filter<WorkMessage> accepts;

        private AtomicBoolean stopped = new AtomicBoolean(false);

        private AtomicInteger messageCount = new AtomicInteger(0);

        private RepositoryChannel(Filter<WorkMessage> accepts) {
            this.accepts = accepts;
            this.receiver = createFiber();
            this.channel = new MemoryChannel<WorkMessage>();
            channel.subscribe(receiver, this);
            //channel.subscribe(receiver, repoService);
            receiver.start();
        }

        public void publish(WorkMessage message) {
            if (stopped.get()) {
                log.debug("worker service has been stopped, rejected message {}", message);
                return;
            }
            if (accepts.passes(message)) {
                log.debug("publishing message {}", message);
                channel.publish(message);
                messageCount.incrementAndGet();
            } else if (log.isDebugEnabled()) {
                log.debug("rejected message {}", message);
            }
        }

        public void onMessage(WorkMessage message) {
            log.debug("Received message {}", message);
            try {
                ArtifactoryContextThreadBinder.bind(context);
                message.call();
            } catch (Throwable t) {
                // not catching exception causes the worker thread to die!
                log.error("Error executing: " + message, t);
                return;
            } finally {
                ArtifactoryContextThreadBinder.unbind();
                int workLeft = messageCount.decrementAndGet();
                if (stopped.get() && workLeft == 0) {
                    synchronized (this) {
                        // wake up the thread waiting on the dispose method if there is one
                        notify();
                    }
                }
            }
            log.debug("Finished processing message {}", message);
        }

        public void stopPublish() {
            if (stopped.compareAndSet(false, true)) {
                // Futher publish goes nowhere
                ((MemoryChannel<WorkMessage>) channel).clearSubscribers();
            }
        }

        public void dispose() {
            Thread.yield();
            if (!allJobsDone()) {
                synchronized (this) {
                    try {
                        wait(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!allJobsDone()) {
                log.debug("Error: Messages on stream are not being closed!\n" +
                        "Open messages count: " + messageCount.get());
            }
            // Dispose of working thread
            receiver.dispose();
        }

        private boolean allJobsDone() {
            return messageCount.get() == 0;
        }
    }
}
