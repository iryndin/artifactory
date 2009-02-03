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
package org.artifactory.webapp.wicket.security.acls;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.security.SecurityHelper;
import org.artifactory.webapp.wicket.AuthenticatedPage;

public class AclsPage extends AuthenticatedPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(AclsPage.class);

    public AclsPage() {
        PermissionTargetsManagementPanel targetsManagementPanel =
                new PermissionTargetsManagementPanel("targetsManagementPanel");
        add(targetsManagementPanel);
        Component targetsTable = targetsManagementPanel.get("targets");
        WebMarkupContainer recipientsPanel =
                (WebMarkupContainer) (targetsManagementPanel.get("panel:recipients"));
        NewPermissionTargetPanel newPermissionTargetPanel =
                new NewPermissionTargetPanel("newTargetPanel", targetsTable, recipientsPanel);
        //Only allow creation of new permission targets to admins
        boolean admin = SecurityHelper.isAdmin();
        if (!admin) {
            newPermissionTargetPanel.setEnabled(false);
            newPermissionTargetPanel.setVisible(false);
        }
        add(newPermissionTargetPanel);
    }

    protected String getPageName() {
        return "Permissions Management";
    }
}