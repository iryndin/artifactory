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
package org.artifactory.config;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.ArtifactoryContextThreadBinder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class CentralConfigFactoryBean
        implements FactoryBean, InitializingBean, ApplicationContextAware {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(CentralConfigFactoryBean.class);

    private CentralConfig cc;

    private ArtifactoryApplicationContext artifactoryContext;

    public CentralConfig getObject() throws Exception {
        return cc;
    }

    public Class getObjectType() {
        return CentralConfig.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.artifactoryContext = (ArtifactoryApplicationContext) applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        File configFilePath = ArtifactoryHome.getConfigFile();
        LOGGER.info("Loading configuration (using '" + configFilePath + "')...");
        //Try to get it from the path
        LOGGER.info("Trying to load configuration from regular path file resource....");
        InputStream in;
        try {
            in = new FileInputStream(configFilePath);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not load configuration from path location '"
                    + configFilePath + "'.");
            throw new RuntimeException("Artifactory configuration load failure!");
        }
        try {
            ArtifactoryContextThreadBinder.bind(artifactoryContext);
            cc = new JaxbHelper<CentralConfig>().read(in, CentralConfig.class);
            LOGGER.info("Loaded configuration from '" + configFilePath + "'.");
            cc.init(artifactoryContext);
            //If we read the configuration from a local file, store it transiently so that we
            //can overwrite the configuration on system import
            cc.setConfigFilePath(configFilePath.getAbsolutePath());
            //TODO: [by yl] REMOVE ME
            /*
            MavenWrapper wrapper = cc.getMavenWrapper();
            Artifact artifact = wrapper.createArtifact("groovy", "groovy", "1.0-jsr-06",
                    Artifact.SCOPE_COMPILE, "jar");
            wrapper.resolve(artifact, cc.getLocalRepositories().get(0), cc.getRemoteRepositories());
            */
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration from '" + configFilePath + "'.", e);
            throw new RuntimeException(e);
        } finally {
            ArtifactoryContextThreadBinder.unbind();
            IOUtils.closeQuietly(in);
        }
    }
}
