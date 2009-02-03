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
package org.artifactory.webapp.wicket.security.users;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.CreateUpdatePanel;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.markup.html.WebMarkupContainer;

@AuthorizeInstantiation("ADMIN")
public class UsersPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(UsersPage.class);


    public UsersPage() {
        WebMarkupContainer actionContainer = new WebMarkupContainer("action");
        actionContainer.setOutputMarkupId(true);
        final UserPanel createPanel =
                new UserPanel("create", CreateUpdatePanel.CreateUpdateAction.CREATE, new User());
        final UserPanel updatePanel =
                new UserPanel("update", CreateUpdatePanel.CreateUpdateAction.UPDATE, new User());
        createPanel.setOtherPanel(updatePanel);
        updatePanel.setOtherPanel(createPanel);
        updatePanel.setVisible(false);
        actionContainer.add(createPanel);
        actionContainer.add(updatePanel);
        add(actionContainer);
        UsersListPanel usersListPanel = new UsersListPanel("usersList", createPanel, updatePanel);
        add(usersListPanel);
        //Update the results when a user is created/updated
        createPanel.setChangeListener(usersListPanel.get("users"));
        updatePanel.setChangeListener(usersListPanel.get("users"));
    }

    protected String getPageName() {
        return "Users Management";
    }

}
