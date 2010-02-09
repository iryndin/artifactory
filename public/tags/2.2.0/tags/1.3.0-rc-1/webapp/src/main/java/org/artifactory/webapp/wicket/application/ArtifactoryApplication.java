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
package org.artifactory.webapp.wicket.application;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.*;
import org.apache.wicket.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadWebRequest;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.settings.ISecuritySettings;
import org.apache.wicket.util.time.Duration;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.webapp.spring.ArtifactorySpringComponentInjector;
import org.artifactory.webapp.wicket.application.sitemap.ArtifactorySiteMapBuilder;
import org.artifactory.webapp.wicket.application.sitemap.MenuNode;
import org.artifactory.webapp.wicket.application.sitemap.SiteMap;
import org.artifactory.webapp.wicket.application.sitemap.SiteMapBuilder;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.SimpleRepoBrowserPage;
import org.artifactory.webapp.wicket.page.error.AccessDeniedPage;
import org.artifactory.webapp.wicket.page.error.InternalErrorPage;
import org.artifactory.webapp.wicket.page.error.PageExpiredErrorPage;
import org.artifactory.webapp.wicket.page.error.SessionExpiredPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;
import org.artifactory.webapp.wicket.page.security.login.LogoutPage;
import org.artifactory.webapp.wicket.page.security.profile.ProfilePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApplication extends AuthenticatedWebApplication {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryApplication.class);

    private SiteMap siteMap;

    public static ArtifactoryApplication get() {
        return (ArtifactoryApplication) Application.get();
    }

    public SiteMap getSiteMap() {
        return siteMap;
    }

    @Override
    protected void init() {
        super.init();

        setup();
        buildSiteMap();
        mountPages();
        deleteUploadsFolder();
    }

    protected void setup() {
        setupListeners();

        // look for pages at the root of the web-app
        getResourceSettings().addResourceFolder("");

        // extend the request timeout to support long running transactions
        getRequestCycleSettings().setTimeout(Duration.hours(5));

        getApplicationSettings().setPageExpiredErrorPage(SessionExpiredPage.class);
        getApplicationSettings().setPageExpiredErrorPage(PageExpiredErrorPage.class);
        getApplicationSettings().setAccessDeniedPage(AccessDeniedPage.class);
        getApplicationSettings().setInternalErrorPage(InternalErrorPage.class);

        getMarkupSettings().setCompressWhitespace(true);
        getMarkupSettings().setStripComments(true);
        getMarkupSettings().setStripWicketTags(true);
    }

    private void setupListeners() {
        //Add the spring injector
        addComponentInstantiationListener(new ArtifactorySpringComponentInjector(this));

        // wrap the unauthorizedComponentInstantiation listener so that we can discard repoPath
        // attributes from the request when needing to login
        ISecuritySettings securitySettings = getSecuritySettings();
        IUnauthorizedComponentInstantiationListener orig =
                securitySettings.getUnauthorizedComponentInstantiationListener();

        securitySettings.setUnauthorizedComponentInstantiationListener(
                new RepoBrowsingAwareUnauthorizedComponentInstantiationListener(orig));
    }

    private void mountPages() {
        mountBookmarkablePage(SimpleRepoBrowserPage.PATH, SimpleRepoBrowserPage.class);

        // mount general pages
        mountPage(InternalErrorPage.class);
        mountPage(AccessDeniedPage.class);
        mountPage(PageExpiredErrorPage.class);
        mountPage(SessionExpiredPage.class);

        mountPage(LoginPage.class);
        mountPage(LogoutPage.class);
        mountPage(ProfilePage.class);

        for (MenuNode pageNode : siteMap.getPages()) {
            if (pageNode.getUrl() != null) {
                mountBookmarkablePage(pageNode.getUrl(), pageNode.getPageClass());
            } else {
                mountPage(pageNode.getPageClass());
            }
        }
    }

    private void buildSiteMap() {
        SiteMapBuilder builder = newSiteMapBuilder();
        builder.createSiteMap();
        builder.buildSiteMap();
        builder.cachePageNodes();
        siteMap = builder.getSiteMap();
    }

    protected SiteMapBuilder newSiteMapBuilder() {
        return new ArtifactorySiteMapBuilder();
    }

    public void mountPage(Class<? extends Page> pageClass) {
        String url =
                "/" + pageClass.getSimpleName().replaceFirst("Page", "").toLowerCase() + ".html";
        mountBookmarkablePage(url, pageClass);
    }

    @Override
    public String getConfigurationType() {
        if (ConstantsValue.devDebugMode.getBoolean()) {
            return DEVELOPMENT;
        }
        return super.getConfigurationType();
    }

    @Override
    protected void onDestroy() {
        deleteUploadsFolder();
        super.onDestroy();
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return ArtifactoryWebSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return LoginPage.class;
    }

    @Override
    public Class getHomePage() {
        return HomePage.class;
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    protected WebRequest newWebRequest(HttpServletRequest servletRequest) {
        return new UploadWebRequest(servletRequest);
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    public RequestCycle newRequestCycle(Request request, Response response) {
        return new ArtifactoryRequestCycle(this, (WebRequest) request, response);
    }

    /**
     * Delete the upload folder (in case we were not shut down cleanly)
     */
    private static void deleteUploadsFolder() {
        File tmpUploadsDir = ArtifactoryHome.getTmpUploadsDir();
        if (tmpUploadsDir.exists()) {
            try {
                FileUtils.cleanDirectory(tmpUploadsDir);
            } catch (IOException ignore) {
                log.warn("Failed to delete the upload directory.");
            }
        }
    }
}
