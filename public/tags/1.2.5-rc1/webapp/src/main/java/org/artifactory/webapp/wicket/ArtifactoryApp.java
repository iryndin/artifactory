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
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadWebRequest;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.util.file.Folder;
import org.apache.wicket.util.time.Duration;
import org.artifactory.repo.RepoBase;
import org.artifactory.webapp.wicket.browse.SimpleRepoBrowserPage;
import org.artifactory.webapp.wicket.error.SessionExpiredPage;
import org.artifactory.webapp.wicket.home.HomePage;
import org.artifactory.webapp.wicket.security.login.LoginPage;

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
        mountBookmarkablePage(SimpleRepoBrowserPage.PATH, SimpleRepoBrowserPage.class);
    }


    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return ArtifactorySession.class;
    }

    protected Class<? extends WebPage> getSignInPageClass() {
        return LoginPage.class;
    }

    @Override
    protected void onDestroy() {
        deleteUploadsFolder();
        super.onDestroy();
    }

    public Class getHomePage() {
        return HomePage.class;
    }

    @Override
    protected WebRequest newWebRequest(HttpServletRequest servletRequest) {
        return new UploadWebRequest(servletRequest);
    }


    @Override
    public RequestCycle newRequestCycle(final Request request, final Response response) {
        return new WebRequestCycle(this, (WebRequest) request, (WebResponse) response) {
            @Override
            protected void onBeginRequest() {
                super.onBeginRequest();
                ArtifactorySession session = ArtifactorySession.get();
                session.attach();
            }
        };
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
