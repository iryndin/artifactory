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
package org.artifactory.webapp.wicket;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.repo.RepoBase;
import org.artifactory.webapp.wicket.error.SessionExpiredPage;
import org.artifactory.webapp.wicket.home.HomePage;
import org.artifactory.webapp.wicket.security.login.LoginPage;
import wicket.authentication.AuthenticatedWebApplication;
import wicket.authentication.AuthenticatedWebSession;
import wicket.extensions.ajax.markup.html.form.upload.UploadWebRequest;
import wicket.markup.html.WebPage;
import wicket.protocol.http.WebRequest;
import wicket.settings.IResourceSettings;
import wicket.util.file.Folder;
import wicket.util.io.IObjectStreamFactory;
import wicket.util.lang.Objects;
import wicket.util.time.Duration;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApp extends AuthenticatedWebApplication {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryApp.class);
    public static final Folder UPLOAD_FOLDER = new Folder(RepoBase.TEMP_FOLDER);

    @Override
    protected void init() {
        super.init();
        IResourceSettings resourceSettings = getResourceSettings();
        //Look for pages at the root of the web-app
        resourceSettings.addResourceFolder("");
        //Delete the upload folder (in case we were not shut down cleanly)
        deleteUploadsFolder();
        //Create the upload folder
        UPLOAD_FOLDER.mkdirs();
        //Extend the request timeout to 5 minutes to support long running transactions
        getRequestCycleSettings().setTimeout(Duration.minutes(5));
        getApplicationSettings().setPageExpiredErrorPage(SessionExpiredPage.class);
        //TODO: [by yl] Remove and let wicket use its default serialization once it's ready
        Objects.setObjectStreamFactory(new IObjectStreamFactory.DefaultObjectStreamFactory());
    }


    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return ArtifactorySession.class;
    }

    protected Class<? extends WebPage> getSignInPageClass() {
        return LoginPage.class;
    }

    @Override
    protected void destroy() {
        deleteUploadsFolder();
        super.destroy();
    }

    public Class getHomePage() {
        return HomePage.class;
    }

    /**
     * @see wicket.protocol.http.WebApplication#newWebRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected WebRequest newWebRequest(HttpServletRequest servletRequest) {
        return new UploadWebRequest(servletRequest);
    }

    private void deleteUploadsFolder() {
        if (UPLOAD_FOLDER.exists()) {
            try {
                FileUtils.deleteDirectory(UPLOAD_FOLDER);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete the upload directory.");
            }
        }
    }
}
