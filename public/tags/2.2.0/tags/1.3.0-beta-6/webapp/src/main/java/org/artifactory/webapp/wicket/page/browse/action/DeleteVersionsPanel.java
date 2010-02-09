package org.artifactory.webapp.wicket.page.browse.action;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.maven.MavenUnitInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.wicket.common.component.CheckboxColumn;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.table.SortableTable;
import org.artifactory.webapp.wicket.utils.ListPropertySorter;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
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
    private RepositoryService repoService;


    public DeleteVersionsPanel(String id, List<DeployableUnit> deployableUnits,
            Component componentToRefresh) {
        super(id);

        Form form = new Form("form");
        add(form);

        MultiMap<String, DeployableUnit> duGroupAndVersion =
                aggregateByGroupAndVersion(deployableUnits);

        dataProvider = new DeployableUnitsDataProvider(duGroupAndVersion);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(
                new CheckboxColumn<DeployableUnitModel>("", "selected", this) {
                    @Override
                    protected void doUpdate(DeployableUnitModel duModel,
                            boolean checked,
                            AjaxRequestTarget target) {
                    }
                });

        columns.add(new PropertyColumn(new Model("GroupId:Version"), "key"));
        columns.add(new PropertyColumn(new Model("Directories Count"), "count"));

        SortableTable table =
                new SortableTable("deployableUnits", columns, dataProvider, 20);
        form.add(table);

        form.add(new ModalCloseLink("cancel"));
        form.add(createSubmitButton(form, componentToRefresh));

    }


    private MultiMap<String, DeployableUnit> aggregateByGroupAndVersion(
            List<DeployableUnit> units) {
        MultiMap<String, DeployableUnit> multiMap = new MultiHashMap<String, DeployableUnit>();
        for (DeployableUnit unit : units) {
            MavenUnitInfo info = unit.getMavenInfo();
            String unitKey = info.getGroupId() + ":" + info.getVersion();
            multiMap.put(unitKey, unit);
        }
        return multiMap;
    }

    private SimpleButton createSubmitButton(Form form, final Component componentToRefresh) {
        SimpleButton submit = new SimpleButton("submit", form, "Delete Selected") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                List<DeployableUnit> selectedDeployableUnits =
                        dataProvider.getSelectedDeploymentUnits();
                if (selectedDeployableUnits.isEmpty()) {
                    error("No version selected for deletion");
                } else {
                    for (DeployableUnit unit : selectedDeployableUnits) {
                        repoService.undeploy(unit.getRepoPath());
                    }
                    info("Selected versions successfully deleted");

                    // colapse all tree nodes
                    if (componentToRefresh instanceof Tree) {
                        // we collapse all since we don't know what tree node the user clicked on (it
                        // is not passed / to the action event and it might not be the currently
                        // selected node if the user right click on a node)
                        // probably will be fixed in 2.0
                        Tree tree = (Tree) componentToRefresh;
                        ITreeState treeState = tree.getTreeState();
                        treeState.collapseAll();
                        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModelObject();
                        treeState.selectNode(((TreeNode) treeModel.getRoot()).getChildAt(0), true);
                    }
                }

                target.addComponent(componentToRefresh);
                FeedbackUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    private class DeployableUnitsDataProvider extends SortableDataProvider {
        private MultiMap<String, DeployableUnit> duGroupAndVersion;
        protected ArrayList<DeployableUnitModel> models;

        private DeployableUnitsDataProvider(MultiMap<String, DeployableUnit> duGroupAndVersion) {
            this.duGroupAndVersion = duGroupAndVersion;
            Set<String> groupVersionKeys = duGroupAndVersion.keySet();
            models = new ArrayList<DeployableUnitModel>(groupVersionKeys.size());
            for (String key : groupVersionKeys) {
                models.add(new DeployableUnitModel(key, duGroupAndVersion.get(key).size()));
            }
            setSort("key", true);
        }

        public Iterator iterator(int first, int count) {
            List<DeployableUnitModel> dusSubList = models.subList(first, first + count);
            ListPropertySorter.sort(dusSubList, getSort());
            return dusSubList.iterator();
        }

        public int size() {
            return models.size();
        }

        public IModel model(Object object) {
            return new Model((DeployableUnitModel) object);
        }

        public List<DeployableUnit> getSelectedDeploymentUnits() {
            List<DeployableUnit> selectedDeploymentUnits = new ArrayList<DeployableUnit>();
            for (DeployableUnitModel model : models) {
                if (model.isSelected()) {
                    selectedDeploymentUnits.addAll(duGroupAndVersion.get(model.getKey()));
                }
            }
            return selectedDeploymentUnits;
        }
    }

    private class DeployableUnitModel implements Serializable {
        private String key;
        private int count;
        private boolean selected;

        private DeployableUnitModel(String key, int count) {
            this.key = key;
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
