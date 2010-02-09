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
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySpringComponentInjector extends SpringComponentInjector {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactorySpringComponentInjector.class);

    public ArtifactorySpringComponentInjector(WebApplication webapp) {
        //We override the logic of locating the application context since the default logic checks
        //that the context is a WebApplicationContext instance which we aren't (we wish to keep the
        //context generic for testing)
        super(webapp, get(webapp));
    }

    private static ApplicationContext get(WebApplication webapp) {
        ServletContext sc = webapp.getServletContext();
        ApplicationContext ac = (ApplicationContext) sc.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        return ac;
    }
}
