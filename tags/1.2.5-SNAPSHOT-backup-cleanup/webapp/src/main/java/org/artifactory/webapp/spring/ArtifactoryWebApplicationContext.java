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
package org.artifactory.webapp.spring;

import org.apache.log4j.Logger;
import org.artifactory.backup.BackupHelper;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.keyval.KeyVals;
import org.artifactory.maven.Maven;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryWebApplicationContext extends XmlWebApplicationContext
        implements ArtifactoryContext {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactoryWebApplicationContext.class);

    public CentralConfig getCentralConfig() {
        return beanForType(CentralConfig.class);
    }

    public SecurityHelper getSecurity() {
        return beanForType(SecurityHelper.class);
    }

    public Maven getMaven() {
        return beanForType(Maven.class);
    }

    public BackupHelper getBackup() {
        return beanForType(BackupHelper.class);
    }

    public KeyVals getKeyVal() {
        return beanForType(KeyVals.class);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T beanForType(Class<T> type) {
        Iterator iter = getBeansOfType(type).values().iterator();
        if (!iter.hasNext()) {
            throw new RuntimeException("Could not find bean of type '" + type.getName() + "'.");
        }
        return (T) iter.next();
    }

    public JcrHelper getJcr() {
        return getCentralConfig().getJcr();
    }

    @Override
    protected void onRefresh() {
        //Do nothing
    }
}
