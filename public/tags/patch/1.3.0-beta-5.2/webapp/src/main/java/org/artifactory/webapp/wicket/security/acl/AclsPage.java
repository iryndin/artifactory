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
package org.artifactory.webapp.wicket.security.acl;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.webapp.wicket.AuthenticatedPage;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;

public class AclsPage extends AuthenticatedPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(AclsPage.class);
    private Component targetsTable;

    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private AclService aclService;

    private WebMarkupContainer createUpdateContainer;

    public AclsPage() {
        PermissionTargetsManagementPanel targetsManagementPanel =
                new PermissionTargetsManagementPanel("targetsManagementPanel");
        add(targetsManagementPanel);
        targetsTable = targetsManagementPanel.get("targets");

        // container to allow replacing the careteUpdate panel
        createUpdateContainer = new WebMarkupContainer("createUpdateContainer");
        createUpdateContainer.setOutputMarkupId(true);
        add(createUpdateContainer);

        PermissionTargetCreateUpdatePanel createPanel = newCreatePanel();
        createUpdateContainer.add(createPanel);
    }

    protected String getPageName() {
        return "Permissions Management";
    }

    PermissionTargetCreateUpdatePanel newCreatePanel() {
        PermissionTargetCreateUpdatePanel panel = new PermissionTargetCreateUpdatePanel(
                CreateUpdatePanel.CreateUpdateAction.CREATE,
                new PermissionTargetInfo(),
                this.targetsTable);
        applyCreatePanelSecurity(panel);
        return panel;
    }
    
    public void newUpdatePanel(PermissionTargetInfo permissionTarget, AjaxRequestTarget target) {
                PermissionTargetCreateUpdatePanel panel = new PermissionTargetCreateUpdatePanel(
                CreateUpdatePanel.CreateUpdateAction.UPDATE,
                permissionTarget,
                targetsTable
        );
        applyUpdateSecurity(panel, permissionTarget);
        createUpdateContainer.replace(panel);
        target.addComponent(createUpdateContainer);
    }

    private void applyCreatePanelSecurity(PermissionTargetCreateUpdatePanel panel) {
        //Only allow creation of new permission targets to system admins
        if (!authService.isAdmin()) {
            disableCreateUpdatePanel(panel);
        }
    }

    private void applyUpdateSecurity(PermissionTargetCreateUpdatePanel panel,
            PermissionTargetInfo target) {
        //Only allow updating permission targets to permission target admins and system admins
        if (!aclService.canAdmin(target)) {
            disableCreateUpdatePanel(panel);
        }
    }

    private void disableCreateUpdatePanel(PermissionTargetCreateUpdatePanel panel) {
        panel.setEnabled(false);
        panel.setVisibilityAllowed(false);
    }

}