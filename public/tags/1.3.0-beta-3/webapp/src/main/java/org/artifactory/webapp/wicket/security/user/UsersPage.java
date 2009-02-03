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
package org.artifactory.webapp.wicket.security.user;

import org.apache.log4j.Logger;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.AuthenticatedPage;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;

@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class UsersPage extends AuthenticatedPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(UsersPage.class);
    private UsersListPanel usersListPanel;

    public UsersPage() {
        usersListPanel = new UsersListPanel("usersList");
        add(usersListPanel);
        UserCreateUpdatePanel createPanel = newCreatePanel();
        add(createPanel);
    }

    public UserCreateUpdatePanel newCreatePanel() {
        return new UserCreateUpdatePanel(
                CreateUpdatePanel.CreateUpdateAction.CREATE, new UserModel(), usersListPanel);
    }

    public UserCreateUpdatePanel newUpdatePanel(UserModel user) {
        return new UserCreateUpdatePanel(
                CreateUpdatePanel.CreateUpdateAction.UPDATE, user, usersListPanel);
    }

    protected String getPageName() {
        return "Users Management";
    }
}
