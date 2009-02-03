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
package org.artifactory.spring;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.security.SecurityService;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.schedule.TaskService;
import org.springframework.context.ApplicationContext;

/**
 * User: freds Date: Aug 3, 2008 Time: 6:48:52 PM
 */
public interface InternalArtifactoryContext extends ArtifactoryContext, ReloadableBean, ApplicationContext {
    JcrService getJcrService();

    JcrRepoService getJcrRepoService();

    SecurityService getSecurityService();

    void addReloadableBean(Class<? extends ReloadableBean> interfaceClass);

    boolean isReady();

    TaskService getTaskService();

}
