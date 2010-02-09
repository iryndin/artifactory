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

import org.artifactory.api.repo.Lock;
import org.artifactory.descriptor.config.CentralConfigDescriptor;

/**
 * User: freds Date: Jul 21, 2008 Time: 11:43:00 AM
 */
public interface ReloadableBean {
    /**
     * This init will be called after the context is created and can be annotated with transactional propagation
     */
    @Lock(transactional = true)
    void init();

    /**
     * List the others reloadable beans that need to be initialized before this one. The final init order will be used
     * in reverse for destroy.
     *
     * @return
     */
    Class<? extends ReloadableBean>[] initAfter();

    /**
     * This is called when the configuration xml changes. It is using the same init order all beans that need to do
     * something on reload.
     *
     * @param oldDescriptor
     */
    void reload(CentralConfigDescriptor oldDescriptor);

    /**
     * Called when Artifactory is shutting down. Called in reverse order than the init order.
     */
    void destroy();
}
