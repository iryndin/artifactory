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
package org.artifactory.schedule.jetlang;

import org.artifactory.schedule.TaskBase;
import org.jetlang.core.RunnableExecutorImpl;
import org.jetlang.fibers.ThreadFiber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author yoavl
 */
public class RepeatingDelayTask extends TaskBase {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(RepeatingDelayTask.class);

    private final RepeatingDelayCommand command;
    private final long interval;
    private final long initialDelay;
    private ThreadFiber fiber;

    public RepeatingDelayTask(RepeatingDelayCommand command, long interval, long initialDelay) {
        super(command.getClass());
        this.command = command;
        this.interval = interval;
        this.initialDelay = initialDelay;
    }

    /**
     * Schedule the task
     */
    @Override
    protected void scheduleTask() {
        //Create the fiber for processing the task
        fiber = new ThreadFiber(new RunnableExecutorImpl(), getName(), false);
        /*Channel<C> channel = new MemoryChannel<C>();
        channel.subscribe(fiber, callback);
        //Send an empty message for the callback to consume
        channel.publish(null);*/
        command.getWorkContext().setTaskToken(getToken());
        fiber.scheduleWithFixedDelay(command, initialDelay, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * Unschedule the task
     */
    @Override
    protected void cancelTask() {
        if (fiber != null) {
            fiber.dispose();
        }
    }

    private String getName() {
        return getType().getName() + "#" + getToken();
    }
}