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
package org.artifactory.rest.servlet;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.spring.container.servlet.SpringComponentProvider;
import org.artifactory.api.context.ArtifactoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import javax.servlet.ServletConfig;

/**
 * We use our own rest servlet for the initialization using ArtifactoryContext.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryRestServlet extends ServletContainer {
    private final static Logger log = LoggerFactory.getLogger(ArtifactoryRestServlet.class);

    @Override
    protected void configure(ServletConfig config, ResourceConfig rc,
            WebApplication application) {
        super.configure(config, rc, application);
    }

    @Override
    protected void initiate(ResourceConfig rc, WebApplication wa) {
        try {
            ArtifactoryContext artifactoryContext =
                    (ArtifactoryContext) getServletContext().getAttribute(
                            "org.springframework.web.context.WebApplicationContext.ROOT");

            wa.initiate(rc, new SpringComponentProvider(
                    (ConfigurableApplicationContext) artifactoryContext));
        } catch (RuntimeException e) {
            log.error("Exception in initialization of the Rest servlet");
            throw e;
        }
    }
}
