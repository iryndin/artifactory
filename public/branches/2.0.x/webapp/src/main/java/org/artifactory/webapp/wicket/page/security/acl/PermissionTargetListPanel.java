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
package org.artifactory.webapp.wicket.page.security.acl;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.list.ListPanel;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class PermissionTargetListPanel extends ListPanel<PermissionTargetInfo> {
    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private AclService security;

    public PermissionTargetListPanel(String id) {
        super(id);

        if (!authService.isAdmin()) {
            disableNewItemLink();
        }

        getDataProvider().setSort("name", true);
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    protected List<PermissionTargetInfo> getList() {
        return security.getAdministrativePermissionTargets();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Permissionn target name"), "name", "name"));
        columns.add(new PropertyColumn(new Model("Repository"), "repoKey", "repoKey"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new PermissionTargetCreateUpdatePanel(
                CreateUpdateAction.CREATE,
                new PermissionTargetInfo(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(PermissionTargetInfo permissionTarget) {
        return new PermissionTargetCreateUpdatePanel(
                CreateUpdateAction.UPDATE,
                permissionTarget, this);
    }

    @Override
    protected String getDeleteConfirmationText(PermissionTargetInfo permissionTarget) {
        return "Are you sure you wish to delete the target " +
                permissionTarget.getName() + "?";
    }

    @Override
    protected void deleteItem(PermissionTargetInfo permissionTarget, AjaxRequestTarget target) {
        security.deleteAcl(permissionTarget);
    }

}
