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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.security.ExtendedJdbcAclDao;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.AjaxDeleteRow;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class PermissionTargetsManagementPanel extends TitlePanel {

    public PermissionTargetsManagementPanel(String string) {
        super(string);
        final WebMarkupContainer pathRecipientsPanelContainer = new WebMarkupContainer("panel");
        pathRecipientsPanelContainer.setOutputMarkupId(true);
        final PermissionTargetRecipientsPanel permissionTargetRecipientsPanel =
                new PermissionTargetRecipientsPanel("recipients");
        permissionTargetRecipientsPanel.setVisible(false);
        pathRecipientsPanelContainer.add(permissionTargetRecipientsPanel);
        add(pathRecipientsPanelContainer);
        //Results table
        final SortablePermissionTargetsDataProvider dataProvider =
                new SortablePermissionTargetsDataProvider();
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Repository:Path Prefix"), "id", "id"));
        columns.add(new AbstractColumn(new Model()) {
            public void populateItem(final Item cellItem, String componentId, IModel model) {
                cellItem.add(new AjaxDeleteRow<RepoPath>(componentId, model,
                        permissionTargetRecipientsPanel) {

                    protected void doDelete() {
                        ArtifactoryContext context = ContextHelper.get();
                        SecurityHelper security = context.getSecurity();
                        ExtendedJdbcAclDao aclDao = security.getAclDao();
                        RepoPath permissionTarget = getToBeDeletedObject();
                        aclDao.delete(permissionTarget);
                    }

                    protected void onDeleted(AjaxRequestTarget target, Component listener) {
                        PermissionTargetRecipientsPanel panel =
                                (PermissionTargetRecipientsPanel) listener;
                        if (panel.isVisible()) {
                            panel.setVisible(false);
                            target.addComponent(panel);
                        }
                        Component table = cellItem.getPage().get("targetsManagementPanel:targets");
                        dataProvider.fetchRepoPaths();
                        target.addComponent(table);
                    }

                    protected String getConfirmationQuestion() {
                        String path = getToBeDeletedObject().getPath();
                        return "Are you sure you wish to delete the target " + path + "?";
                    }
                });
            }
        });
        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("targets", columns, dataProvider, 10) {
                    //Handle row selection
                    @Override
                    protected Item newCellItem(final String id, int index, final IModel model) {
                        Item item = super.newCellItem(id, index, model);
                        Object modelObject = model.getObject();
                        if (modelObject instanceof PropertyColumn) {
                            //Don't add behavior to the delete column
                            item.add(new AjaxEventBehavior("onMouseUp") {
                                protected void onEvent(final AjaxRequestTarget target) {
                                    //Show the update panel
                                    RepoPath ragi =
                                            (RepoPath) getComponent().getParent().getParent()
                                                    .getModelObject();
                                    permissionTargetRecipientsPanel.updateModelObject(
                                            ragi, pathRecipientsPanelContainer, target);
                                }
                            });
                        }
                        return item;
                    }

                    @Override
                    protected void onModelChanged() {
                        dataProvider.fetchRepoPaths();
                    }
                };
        add(table);
    }

    private class SortablePermissionTargetsDataProvider extends SortableDataProvider {

        private List<RepoPath> repoPaths;

        public SortablePermissionTargetsDataProvider() {
            //Set default sort
            setSort("id", true);
            //Load the paths
            fetchRepoPaths();
        }

        public Iterator iterator(int first, int count) {
            SortParam sp = getSort();
            boolean asc = sp.isAscending();
            if (asc) {
                Collections.sort(repoPaths, new Comparator<RepoPath>() {
                    public int compare(RepoPath g1, RepoPath g2) {
                        return g1.getId().compareTo(g2.getId());
                    }
                });
            } else {
                Collections.sort(repoPaths, new Comparator<RepoPath>() {
                    public int compare(RepoPath g1, RepoPath g2) {
                        return g2.getId().compareTo(g1.getId());
                    }
                });
            }
            List<RepoPath> list = repoPaths.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return repoPaths.size();
        }

        public IModel model(Object object) {
            return new Model((RepoPath) object);
        }

        @SuppressWarnings({"UnnecessaryLocalVariable"})
        private List<RepoPath> fetchRepoPaths() {
            ArtifactoryContext context = ContextHelper.get();
            SecurityHelper security = context.getSecurity();
            List<RepoPath> repoPaths = security.getAclDao().getAllRepoPaths();
            //Only return the repoPaths the user is an admin of
            this.repoPaths = new ArrayList<RepoPath>();
            for (RepoPath repoPath : repoPaths) {
                if (security.canAdmin(repoPath)) {
                    this.repoPaths.add(repoPath);
                }
            }
            return this.repoPaths;
        }
    }
}
