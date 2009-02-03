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
package org.artifactory.scheduling;

import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySchedulerFactoryBean extends SchedulerFactoryBean implements
        ApplicationContextAware {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactorySchedulerFactoryBean.class);

    /**
     * Ugly signleton for use by quartz only
     */
    private static ArtifactoryContext signleton;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        //Initi the static singleton
        ArtifactorySchedulerFactoryBean.signleton = (ArtifactoryContext) applicationContext;
    }

    @Override
    public void destroy() throws SchedulerException {
        //Clean up the static singleton
        ArtifactorySchedulerFactoryBean.signleton = null;
        super.destroy();
    }

    static ArtifactoryContext getSingletonContext() {
        return signleton;
    }
}
