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

import org.artifactory.security.SecurityHelper;
import org.artifactory.webapp.wicket.browse.BrowseRepoPage;
import org.artifactory.webapp.wicket.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.importexport.ImportExportPage;
import org.artifactory.webapp.wicket.search.ArtifactLocatorPage;
import org.artifactory.webapp.wicket.security.acls.AclsPage;
import org.artifactory.webapp.wicket.security.login.LogoutPage;
import org.artifactory.webapp.wicket.security.profile.ProfilePage;
import org.artifactory.webapp.wicket.security.users.UsersPage;
import org.artifactory.webapp.wicket.widget.SecuredPageLink;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.html.link.PageLink;
import wicket.model.PropertyModel;

@AuthorizeInstantiation("USER")
public abstract class ArtifactoryPage extends BasePage {

    public ArtifactoryPage() {
        //Add page links
        add(new PageLink("locatePage", ArtifactLocatorPage.class));
        add(new PageLink("browsePage", BrowseRepoPage.class));
        PageLink deployLink = new SecuredPageLink("deployPage", DeployArtifactPage.class);
        add(deployLink);
        PageLink importExportLink = new SecuredPageLink("importExportPage", ImportExportPage.class);
        add(importExportLink);
        PageLink usersLink = new SecuredPageLink("usersPage", UsersPage.class);
        add(usersLink);
        PageLink aclsLink = new SecuredPageLink("aclsPage", AclsPage.class);
        add(aclsLink);
        //Only allow users with updatable profile to change their
        add(new SecuredPageLink("profilePage", ProfilePage.class) {
            @Override
            public boolean isEnabled() {
                boolean updatableProfile = SecurityHelper.isUpdatableProfile();
                return updatableProfile && super.isEnabled();
            }
        });
        add(new BookmarkablePageLink("logoutPage", LogoutPage.class));
        add(new Label("pageTitle", new PropertyModel(this, "pageTitle")));
        add(new Label("username", new PropertyModel(this, "userName")));
    }

    protected abstract String getPageName();

    public String getPageTitle() {
        String serverName = getCc().getServerName();
        String pageName = getPageName();
        return "Artifactory@" + serverName + " :: " + pageName;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public String getUserName() {
        return SecurityHelper.getUsername();
    }
}

