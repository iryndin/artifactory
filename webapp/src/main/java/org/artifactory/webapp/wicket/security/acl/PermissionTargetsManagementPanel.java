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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.component.AjaxDeleteRow;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.component.table.SingleSelectionTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class PermissionTargetsManagementPanel extends TitledPanel {

    @SpringBean
    private AclService security;

    public PermissionTargetsManagementPanel(String string) {
        super(string);

        //Results table
        final SortablePermissionTargetsDataProvider dataProvider =
                new SortablePermissionTargetsDataProvider();
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Permissionn target name"), "name",
                "name"));
        columns.add(new DeleteColumn(dataProvider));
        AjaxFallbackDefaultDataTable table =
                new AjaxPermissionsDataTable(columns, dataProvider);
        add(table);
    }

    private AclsPage getAclPage() {
        return (AclsPage) getPage();
    }

    private CreateUpdatePanel<PermissionTargetInfo> getCreateUpdatePanel() {
        return (PermissionTargetCreateUpdatePanel) getAclPage()
                .get("createUpdateContainer:createUpdate");
    }

    private class SortablePermissionTargetsDataProvider extends SortableDataProvider {

        private List<PermissionTargetInfo> permissionTargets;

        public SortablePermissionTargetsDataProvider() {
            //Set default sort
            setSort("id", true);
            //Load the paths
            fetchPermissionTargets();
        }

        public Iterator iterator(int first, int count) {
            SortParam sp = getSort();
            boolean asc = sp.isAscending();
            if (asc) {
                Collections.sort(permissionTargets, new Comparator<PermissionTargetInfo>() {
                    public int compare(PermissionTargetInfo g1, PermissionTargetInfo g2) {
                        return g1.getName().compareTo(g2.getName());
                    }
                });
            } else {
                Collections.sort(permissionTargets, new Comparator<PermissionTargetInfo>() {
                    public int compare(PermissionTargetInfo g1, PermissionTargetInfo g2) {
                        return g2.getName().compareTo(g1.getName());
                    }
                });
            }
            List<PermissionTargetInfo> list = permissionTargets.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return permissionTargets.size();
        }

        public IModel model(Object object) {
            return new Model((PermissionTargetInfo) object);
        }

        private void fetchPermissionTargets() {
            permissionTargets = security.getAdministrativePermissionTargets();
        }
    }

    private class DeleteColumn extends AbstractColumn {
        private final SortablePermissionTargetsDataProvider dataProvider;

        public DeleteColumn(
                SortablePermissionTargetsDataProvider dataProvider) {
            super(new Model());
            this.dataProvider = dataProvider;
        }

        public void populateItem(final Item cellItem, String componentId, IModel model) {
            cellItem.add(new CssClass("DeleteColumn"));
            cellItem.add(new AjaxDeleteRow<PermissionTargetInfo>(componentId, model, null) {

                @Override
                protected void doDelete() {
                    PermissionTargetInfo permissionTarget = getToBeDeletedObject();
                    security.deleteAcl(permissionTarget);
                }

                @Override
                protected void onDeleted(AjaxRequestTarget target, Component listener) {
                    Component table = cellItem.getPage().get("targetsManagementPanel:targets");
                    dataProvider.fetchPermissionTargets();
                    target.addComponent(table);
                    //Hide the update panel
                    getCreateUpdatePanel().replaceWith(target, (getAclPage().newCreatePanel()));
                }

                @Override
                protected String getConfirmationQuestion() {
                    String name = getToBeDeletedObject().getName();
                    return "Are you sure you wish to delete the target " + name + "?";
                }
            });
        }
    }

    private class AjaxPermissionsDataTable extends SingleSelectionTable<PermissionTargetInfo> {
        private final SortablePermissionTargetsDataProvider dataProvider;

        public AjaxPermissionsDataTable(List<IColumn> columns,
                SortablePermissionTargetsDataProvider dataProvider) {
            super("targets", columns, dataProvider, 10);
            this.dataProvider = dataProvider;
        }

        @Override
        protected void onRowSelected(PermissionTargetInfo selection, AjaxRequestTarget target) {
            super.onRowSelected(selection, target);

            (getAclPage()).newUpdatePanel(selection, target);
        }

        @Override
        protected void onModelChanged() {
            dataProvider.fetchPermissionTargets();
        }
    }
}
