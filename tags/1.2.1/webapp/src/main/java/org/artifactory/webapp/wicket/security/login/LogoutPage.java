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
package org.artifactory.webapp.wicket.security.login;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactorySession;
import org.artifactory.webapp.wicket.BasePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class LogoutPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LogoutPage.class);

    public LogoutPage() {
        add(new LogoutPanel("logoutPanel"));
    }

    @Override
    protected void onBeforeRender() {
        //Signout
        ArtifactorySession session = ArtifactorySession.get();
        session.signOut();
    }
}
