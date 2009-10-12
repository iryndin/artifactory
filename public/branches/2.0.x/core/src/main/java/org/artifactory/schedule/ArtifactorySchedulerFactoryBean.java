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
package org.artifactory.schedule;

import org.artifactory.spring.InternalArtifactoryContext;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.annotation.PreDestroy;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySchedulerFactoryBean extends SchedulerFactoryBean {

    /**
     * Ugly signleton for use by quartz only
     */
    private static InternalArtifactoryContext singleton;

    @Autowired
    public void setApplicationContext(InternalArtifactoryContext applicationContext) {
        super.setApplicationContext(applicationContext);
        //Init the static singleton
        ArtifactorySchedulerFactoryBean.singleton = applicationContext;
    }

    @PreDestroy
    @Override
    public void destroy() throws SchedulerException {
        //Clean up the static singleton
        ArtifactorySchedulerFactoryBean.singleton = null;
        super.destroy();
    }

    /**
     * TODO: Ugly singleton because need to get the context in the quartz thread
     *
     * @return
     */
    @Deprecated
    public static InternalArtifactoryContext getContext() {
        return singleton;
    }
}
