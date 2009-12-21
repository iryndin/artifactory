/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.browse.treebrowser.action;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.BuildAddon;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.maven.MavenUnitInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.component.confirm.AjaxConfirm;
import org.artifactory.common.wicket.component.confirm.ConfirmDialog;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.checkbox.SelectAllCheckboxColumn;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.wicket.actionable.tree.ActionableItemTreeNode;
import org.artifactory.webapp.wicket.actionable.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This panel allows the user to select a group of deployable artifacts to delete.
 *
 * @author Yossi Shaul
 */
public class DeleteVersionsPanel extends Panel {
    private DeployableUnitsDataProvider dataProvider;

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private RepositoryService repoService;

    public DeleteVersionsPanel(String id, List<DeployableUnit> deployableUnits, TreeBrowsePanel browseRepoPanel,
            RepoAwareActionableItem source) {
        super(id);

        Form form = new Form("form");
        add(form);

        MultiMap<String, DeployableUnit> duGroupAndVersion = aggregateByGroupAndVersion(deployableUnits);

        dataProvider = new DeployableUnitsDataProvider(duGroupAndVersion);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new SelectAllCheckboxColumn<DeployableUnitModel>("", "selected", null));
        columns.add(new PropertyColumn(new Model("Group Id"), "groupId", "groupId"));
        columns.add(new PropertyColumn(new Model("Version"), "version", "version"));
        columns.add(new PropertyColumn(new Model("Directories Count"), "count"));

        SortableTable table = new SortableTable("deployableUnits", columns, dataProvider, 20);
        form.add(table);

        form.add(new ModalCloseLink("cancel"));
        form.add(createSubmitButton(form, browseRepoPanel, source));
    }


    private MultiMap<String, DeployableUnit> aggregateByGroupAndVersion(List<DeployableUnit> units) {
        MultiMap<String, DeployableUnit> multiMap = new MultiHashMap<String, DeployableUnit>();
        for (DeployableUnit unit : units) {
            MavenUnitInfo info = unit.getMavenInfo();
            String unitKey = toGroupVersionKey(info);
            multiMap.put(unitKey, unit);
        }
        return multiMap;
    }

    private String toGroupVersionKey(MavenUnitInfo info) {
        return info.getGroupId() + ":" + info.getVersion();
    }

    private String groupFromGroupVersionKey(String groupVersionKey) {
        return groupVersionKey.split(":")[0];
    }

    private String versionFromGroupVersionKey(String groupVersionKey) {
        return groupVersionKey.split(":")[1];
    }

    private TitledAjaxSubmitLink createSubmitButton(Form form, final TreeBrowsePanel browseRepoPanel, final
    RepoAwareActionableItem source) {
        TitledAjaxSubmitLink submit = new TitledAjaxSubmitLink("submit", "Delete Selected", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final List<RepoPath> selectedRepoPaths = dataProvider.getSelectedRepoPaths();
                if (selectedRepoPaths.isEmpty()) {
                    error("No version selected for deletion");
                    return; // keep popup open
                }
                AjaxConfirm.get().confirm(new ConfirmDialog() {
                    public String getMessage() {
                        BuildAddon buildAddon = addonsManager.addonByType(BuildAddon.class);
                        return buildAddon.getDeleteVersionsWarningMessage(selectedRepoPaths,
                                "Are you sure you wish to delete the selected versions?");
                    }

                    public void onConfirm(boolean approved, AjaxRequestTarget target) {
                        if (approved) {
                            repoService.undeployPaths(selectedRepoPaths);

                            getPage().info("Selected versions deleted successfully");
                            AjaxUtils.refreshFeedback(target);
                            browseRepoPanel.removeNodePanel(target);
                            ActionableItemsTree tree = browseRepoPanel.getTree();

                            ActionableItemTreeNode itemNode = tree.searchForNodeByItem(source);
                            RepoPath path = source.getRepoPath();
                            if (source instanceof FolderActionableItem) {
                                path = ((FolderActionableItem) source).getCanonicalPath();
                            }
                            ActionableItemTreeNode parent = itemNode.getParent();
                            if (repoService.exists(path)) {
                                tree.getTreeState().collapseNode(itemNode);
                            } else {
                                itemNode.removeFromParent();
                                tree.getTreeState().collapseNode(parent);
                            }

                            target.addComponent(tree);
                            tree.adjustLayout(target);
                            ModalHandler.closeCurrent(target);
                        }
                    }
                });
            }
        };
        return submit;
    }

    private class DeployableUnitsDataProvider extends SortableDataProvider {
        private MultiMap<String, DeployableUnit> duGroupAndVersion;
        protected ArrayList<DeployableUnitModel> duModels;

        private DeployableUnitsDataProvider(MultiMap<String, DeployableUnit> duGroupAndVersion) {
            this.duGroupAndVersion = duGroupAndVersion;
            Set<String> groupVersionKeys = duGroupAndVersion.keySet();
            duModels = new ArrayList<DeployableUnitModel>(groupVersionKeys.size());
            for (String key : groupVersionKeys) {
                duModels.add(new DeployableUnitModel(key, duGroupAndVersion.get(key).size()));
            }
            setSort("groupId", true);
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(duModels, getSort());
            List<DeployableUnitModel> dusSubList = duModels.subList(first, first + count);
            return dusSubList.iterator();
        }

        public int size() {
            return duModels.size();
        }

        public IModel model(Object object) {
            return new Model((DeployableUnitModel) object);
        }

        public List<RepoPath> getSelectedRepoPaths() {
            List<RepoPath> selectedDeploymentUnits = new ArrayList<RepoPath>();
            for (DeployableUnitModel model : duModels) {
                if (model.isSelected()) {
                    for (DeployableUnit deployableUnit : duGroupAndVersion.get(model.getKey())) {
                        selectedDeploymentUnits.add(deployableUnit.getRepoPath());
                    }
                }
            }
            return selectedDeploymentUnits;
        }
    }

    private class DeployableUnitModel implements Serializable {
        private String key;
        private String groupId;
        private String version;
        private int count;
        private boolean selected;

        private DeployableUnitModel(String key, int count) {
            this.key = key;
            this.count = count;
            this.groupId = groupFromGroupVersionKey(key);
            this.version = versionFromGroupVersionKey(key);
        }

        public int getCount() {
            return count;
        }

        public String getKey() {
            return key;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getVersion() {
            return version;
        }
    }
}