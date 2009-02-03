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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.component.CheckboxColumn;
import org.artifactory.webapp.wicket.component.panel.fieldset.FieldSetPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the permissions recipients table.
 *
 * Created by IntelliJ IDEA. User: yoavl
 */
public class PermissionTargetRecipientsPanel extends FieldSetPanel {
    private final static Logger LOGGER = Logger.getLogger(PermissionTargetRecipientsPanel.class);

    @SpringBean
    private AclService aclService;

    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private UserGroupService userGroupService;

    private PermissionTargetInfo permissionTarget;

    private AclInfo aclInfo;
    private PermissionTargetAceInfoRowDataProvider dataProvider;

    public PermissionTargetRecipientsPanel(String id, PermissionTargetInfo permissionTarget) {
        super(id);
        setOutputMarkupId(true);
        this.permissionTarget = permissionTarget;

        aclInfo = aclService.getAcl(permissionTarget);
        dataProvider = new PermissionTargetAceInfoRowDataProvider(userGroupService, aclInfo);
        addPermissionsTable(permissionTarget);
    }

    private void addPermissionsTable(PermissionTargetInfo permissionTarget) {
        //Permissions table
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(
                new Model("Principal"), "principal", "principal") {

            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                super.populateItem(item, componentId, model);
                AceInfoRow aceInfoRow = (AceInfoRow) model.getObject();
                if (aceInfoRow.isGroup()) {
                    item.add(new CssClass("group"));
                } else {
                    item.add(new CssClass("user"));
                }
            }
        });
        columns.add(new CheckboxColumn<AceInfoRow>("Admin", "admin", "admin", this) {
            @Override
            protected void doUpdate(AceInfoRow row, boolean value, AjaxRequestTarget target) {
                if (sanityCheckAdmin() && isEnabled(row)) {
                    row.setAdmin(value);
                    onCheckboxUpdate(target);
                }
            }

            @Override
            protected boolean isEnabled(AceInfoRow row) {
                String currentUsername = authService.currentUsername();
                String username = row.getPrincipal();
                //Do not allow admin user to change (revoke) his admin bit
                return !username.equals(currentUsername);
            }
        });
        columns.add(
                new CheckboxColumn<AceInfoRow>("Delete", "delete", "delete", this) {
                    @Override
                    protected void doUpdate(AceInfoRow row, boolean value,
                            AjaxRequestTarget target) {
                        if (sanityCheckAdmin()) {
                            row.setDelete(value);
                            onCheckboxUpdate(target);
                        }
                    }
                });
        columns.add(
                new CheckboxColumn<AceInfoRow>("Deploy", "deploy", "deploy", this) {
                    @Override
                    protected void doUpdate(AceInfoRow row, boolean value,
                            AjaxRequestTarget target) {
                        if (sanityCheckAdmin()) {
                            row.setDeploy(value);
                            onCheckboxUpdate(target);
                        }
                    }
                });
        columns.add(new CheckboxColumn<AceInfoRow>("Read", "read", "read", this) {
            @Override
            protected void doUpdate(AceInfoRow row, boolean value, AjaxRequestTarget target) {
                if (sanityCheckAdmin()) {
                    row.setRead(value);
                    onCheckboxUpdate(target);
                }
            }
        });
        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("recipients", columns, dataProvider, 5);
        //Recipients header
        Label recipientsHeader = new Label("recipientsHeader");
        recipientsHeader.setModel(
                new Model("Permissions for \"" + permissionTarget.getName() + "\""));

        add(table);
    }

    private void onCheckboxUpdate(AjaxRequestTarget target) {
        target.addComponent(this);
    }

    public void save() {
        aclService.updateAcl(aclInfo);
        reloadData();
    }

    private void reloadData() {
        //Reload from backend
        aclInfo = aclService.getAcl(permissionTarget);
        dataProvider.setAclInfo(aclInfo);
        dataProvider.loadData();
    }

    public void cancel() {
        reloadData();
    }

    public AclInfo getAclInfo() {
        return aclInfo;
    }

    @Override
    public String getTitle() {
        return "Permissions";
    }

    private boolean sanityCheckAdmin() {
        if (!aclService.canAdmin(permissionTarget)) {
            String username = authService.currentUsername();
            LOGGER.error(username +
                    " operation ignored: not enough permissions to administer '" +
                    permissionTarget + "'.");
            return false;
        }
        return true;
    }

}
