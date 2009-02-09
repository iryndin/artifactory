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
package org.artifactory.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author freds
 * @date Sep 22, 2008
 */
public class SessionResourceManagerImpl implements SessionResourceManager {
    private static final Logger log = LoggerFactory.getLogger(SessionResourceManagerImpl.class);

    private final Map<Class, SessionResource> resources = new HashMap<Class, SessionResource>();

    @SuppressWarnings({"unchecked"})
    public <T extends SessionResource> T getOrCreateResource(Class<T> resourceClass) {
        T result = (T) resources.get(resourceClass);
        if (result == null) {
            try {
                result = resourceClass.newInstance();
                resources.put(resourceClass, result);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public boolean hasPendingChanges() {
        for (SessionResource resource : resources.values()) {
            if (resource.hasPendingChanges()) {
                return true;
            }
        }
        return false;
    }

    public void onSessionSave() {
        for (SessionResource resource : resources.values()) {
            resource.onSessionSave();
        }
    }

    public void afterCompletion(boolean commit) {
        RuntimeException ex = null;
        for (SessionResource resource : resources.values()) {
            try {
                resource.afterCompletion(commit);
            } catch (RuntimeException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error while releasing resources " + resource + " : " + e.getMessage(), e);
                }
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    public boolean hasResources() {
        for (SessionResource resource : resources.values()) {
            if (resource.hasResources()) {
                return true;
            }
        }
        return false;
    }
}
