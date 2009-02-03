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

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.browse.BrowseRepoPage;
import org.artifactory.webapp.wicket.browse.VirtualRepoListPage;
import org.artifactory.webapp.wicket.component.SecuredPageLink;
import org.artifactory.webapp.wicket.config.ReloadConfigPage;
import org.artifactory.webapp.wicket.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.importexport.repos.ImportExportReposPage;
import org.artifactory.webapp.wicket.importexport.system.ImportExportSystemPage;
import org.artifactory.webapp.wicket.search.ArtifactSearchPage;
import org.artifactory.webapp.wicket.security.acl.AclsPage;
import org.artifactory.webapp.wicket.security.group.GroupsPage;
import org.artifactory.webapp.wicket.security.login.LoginPage;
import org.artifactory.webapp.wicket.security.login.LogoutPage;
import org.artifactory.webapp.wicket.security.profile.ProfilePage;
import org.artifactory.webapp.wicket.security.user.UsersPage;

@AuthorizeInstantiation(AuthorizationService.ROLE_USER)
public abstract class AuthenticatedPage extends BasePage {

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private AuthorizationService authorizationService;

    public AuthenticatedPage() {
        //Add page links
        add(new SecuredPageLink("locatePage", ArtifactSearchPage.class));
        add(new SecuredPageLink("browsePage", BrowseRepoPage.class));
        add(new SecuredPageLink("browseVirtualPage", VirtualRepoListPage.class));
        add(new SecuredPageLink("usersPage", UsersPage.class));
        add(new SecuredPageLink("groupsPage", GroupsPage.class));
        add(new SecuredPageLink("aclsPage", AclsPage.class));
        add(new SecuredPageLink("importExportReposPage", ImportExportReposPage.class));
        add(new SecuredPageLink("importExportSystemPage", ImportExportSystemPage.class));
        add(new SecuredPageLink("reloadConfigPage", ReloadConfigPage.class));

        add(new SecuredPageLink("deployPage", DeployArtifactPage.class) {
            @Override
            public boolean isEnabled() {
                return authorizationService.canDeploy();
            }
        });

        // Only allow users with updatable profile to change their profile
        add(new SecuredPageLink("profilePage", ProfilePage.class) {
            @Override
            public boolean isVisible() {
                return authorizationService.isUpdatableProfile();
            }
        });

        // Enable only for signed in users
        add(new BookmarkablePageLink("logoutPage", LogoutPage.class) {
            @Override
            public boolean isVisible() {
                return ArtifactoryWebSession.get().isSignedIn() &&
                        !authorizationService.isAnonymous();
            }
        });

        // Enable only if signed in as anonymous
        add(new BookmarkablePageLink("loginPage", LoginPage.class) {
            @Override
            public boolean isVisible() {
                return ArtifactoryWebSession.get().isSignedIn() &&
                        authorizationService.isAnonymous();
            }
        });

        add(new Label("pageTitle", new PropertyModel(this, "pageTitle")));
        add(new Label("username", new PropertyModel(this, "userName")));
    }

    protected abstract String getPageName();

    public String getPageTitle() {
        String serverName = centralConfig.getServerName();
        String pageName = getPageName();
        return "Artifactory@" + serverName + " :: " + pageName;
    }

    public String getUserName() {
        if (authorizationService.isAnonymous()) {
            return "Not logged in";
        }
        return "Logged in as: " + authorizationService.currentUsername();
    }
}

