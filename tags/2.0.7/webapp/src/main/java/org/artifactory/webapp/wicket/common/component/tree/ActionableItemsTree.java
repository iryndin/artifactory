/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.wicket.common.component.tree;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.model.Model;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.ItemNotFoundException;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemActionListener;
import org.artifactory.webapp.actionable.action.RemoveAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.model.HierarchicActionableItem;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.CancelDefaultDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ActionableItemsTree extends Tree implements ItemActionListener {
    private static final Logger log = LoggerFactory.getLogger(ActionableItemsTree.class);

    private final ActionableItemsProvider itemsProvider;

    public ActionableItemsTree(String id, ActionableItemsProvider itemsProvider) {
        super(id);

        this.itemsProvider = itemsProvider;
        setRootLess(true);
        HierarchicActionableItem root = itemsProvider.getRoot();
        ActionableItemTreeNode rootNode = new ActionableItemTreeNode(root);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        setModel(new Model(treeModel));
        List<? extends ActionableItem> children = itemsProvider.getChildren(root);
        setChildren(rootNode, children);
        getTreeState().expandNode(rootNode);
        selectNode(rootNode.getFirstChild());
        setOutputMarkupId(true);
    }

    /**
     * Builds a tree and set the selected path to the input repo path. If the repoPath is null or not found, we use the
     * default view.
     *
     * @param id            The wicket id
     * @param itemsProvider Actionable items provider
     * @param repoPath      The path to select
     */
    public ActionableItemsTree(String id, ActionableItemsProvider itemsProvider,
                               RepoPath repoPath) {
        this(id, itemsProvider);

        if (repoPath == null) {
            return;
        }

        try {
            DefaultTreeModel treeModel = (DefaultTreeModel) getModelObject();
            ActionableItemTreeNode rootNode = (ActionableItemTreeNode) treeModel.getRoot();

            // now build all the nodes on the way to the destination path and
            // expand only the nodes to the destination path
            String treePath = getTreePath(repoPath);
            ActionableItemTreeNode parentNode = rootNode;
            String remainingPath = treePath;
            ActionableItemTreeNode currentNode = null;
            while (PathUtils.hasText(remainingPath)) {

                // get deepest node for the path (will also tale care of compacted paths)
                currentNode = getNodeAt(parentNode, remainingPath);
                if (currentNode == parentNode) {
                    throw new ItemNotFoundException(
                            String.format("Child node %s not found under %s",
                                    remainingPath, parentNode.getUserObject().getDisplayName()));
                }

                ActionableItem userObject = currentNode.getUserObject();
                if (userObject instanceof HierarchicActionableItem) {
                    // the node found is hierarchical, meaning it can have children
                    // so we get and create all the current node children
                    List<? extends ActionableItem> folderChildren = itemsProvider
                            .getChildren((HierarchicActionableItem) userObject);
                    setChildren(currentNode, folderChildren);
                    getTreeState().expandNode(currentNode);
                    parentNode = currentNode;
                }

                // substract the resolved path from the remainng path
                // we are currently relying on the display name as there is
                // no better way to know if the node was compacted or not
                String displayName = userObject.getDisplayName();
                remainingPath = remainingPath.substring(displayName.length());
                // just make sure we don't have '/' at the beginning
                remainingPath = PathUtils.trimLeadingSlashes(remainingPath);
            }

            // everything went well and we have the destination node. now select it
            selectNode(currentNode);

        } catch (Exception e) {
            String message = "Unable to find path " +
                    repoPath.getRepoKey() + ":" + repoPath.getPath();
            error(message);
            log.error(message, e);
            getTreeState().collapseAll();
        }
    }

    public ActionableItemsProvider getItemsProvider() {
        return itemsProvider;
    }

    @Override
    protected Component newNodeIcon(MarkupContainer parent, String id, TreeNode node) {
        WebMarkupContainer icon = new WebMarkupContainer(id);
        ActionableItemTreeNode treeNode = (ActionableItemTreeNode) node;
        ActionableItem item = treeNode.getUserObject();
        icon.add(new CssClass(item.getCssClass()));
        return icon;
    }

    @Override
    protected void populateTreeItem(final WebMarkupContainer item, int level) {
        super.populateTreeItem(item, level);
        item.get("nodeLink:label").add(new CssClass("node-label"));

        item.add(new AjaxEventBehavior("oncontextmenu") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onContextMenu(item, target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new CancelDefaultDecorator();
            }
        });
    }

    protected void onContextMenu(WebMarkupContainer item, AjaxRequestTarget target) {
    }

    @Override
    public void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
        super.onJunctionLinkClicked(target, node);
        boolean expanded = isNodeExpanded(node);
        ActionableItemTreeNode actionableItemTreeNode = (ActionableItemTreeNode) node;
        HierarchicActionableItem item =
                (HierarchicActionableItem) actionableItemTreeNode.getUserObject();
        if (expanded) {
            debugExpand(item, true);
            List<? extends ActionableItem> children = itemsProvider.getChildren(item);
            setChildren(actionableItemTreeNode, children);
            debugExpand(item, false);
        }
        target.appendJavascript("dijit.byId('browseTree').layout();");
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
        super.onNodeLinkClicked(target, node);
        selectNode(node);
        target.addComponent(itemsProvider.getItemDisplayPanel());
    }

    private void selectNode(TreeNode node) {
        ActionableItemTreeNode mutableTreeNode = (ActionableItemTreeNode) node;
        ActionableItem item = mutableTreeNode.getUserObject();
        Panel oldDisplayPanel = itemsProvider.getItemDisplayPanel();
        Panel newDisplayPanel = item.newItemDetailsPanel(oldDisplayPanel.getId());
        newDisplayPanel.setOutputMarkupId(true);
        oldDisplayPanel.replaceWith(newDisplayPanel);
        itemsProvider.setItemDisplayPanel(newDisplayPanel);
        getTreeState().selectNode(node, true);
    }

    @Override
    protected Component newJunctionLink(MarkupContainer parent, String id, String imageId,
                                        TreeNode node) {
        //Collapse empty nodes
        ActionableItemTreeNode mutableTreeNode = (ActionableItemTreeNode) node;
        ActionableItem userObject = mutableTreeNode.getUserObject();
        if (userObject instanceof HierarchicActionableItem) {
            boolean hasChildren = itemsProvider.hasChildren((HierarchicActionableItem) userObject);
            //Must be set before the call to super
            mutableTreeNode.setLeaf(!hasChildren);
        } else {
            mutableTreeNode.setLeaf(true);
        }
        return super.newJunctionLink(parent, id, imageId, node);
    }

    @Override
    public void onTargetRespond(AjaxRequestTarget target) {
        //Overriden just for debugging sake
        if (log.isDebugEnabled()) {
            log.debug("Beginning tree update ajax response.");
        }
        super.onTargetRespond(target);
        if (log.isDebugEnabled()) {
            log.debug("Finished tree update ajax response.");
        }
    }

    public void actionPerformed(ItemEvent e) {
        String command = e.getActionCommand();
        if (RemoveAction.ACTION_NAME.equals(command)) {
            RepoAwareActionableItem item = (RepoAwareActionableItem) e.getSource();
            RepoPath repoPath = item.getRepoPath();
            DefaultTreeModel treeModel = (DefaultTreeModel) getModelObject();
            ActionableItemTreeNode rootNode = (ActionableItemTreeNode) treeModel.getRoot();
            String treePath = getTreePath(repoPath);
            ActionableItemTreeNode node = getNodeAt(rootNode, treePath);
            ActionableItemTreeNode parentNode = node.getParent();
            //Update the parent
            if (parentNode != null) {
                //Do not remove the repositories themselves
                if (parentNode.equals(rootNode)) {
                    node.removeAllChildren();
                } else {
                    node.removeFromParent();
                }
                ITreeState state = getTreeState();
                state.expandNode(parentNode);
            }
            e.getTarget().appendJavascript("dijit.byId('browseTree').layout();");
        }
    }

    /**
     * Returns the deepest node matching the given path. For example if the parent look like parent/child/1 and we ask
     * for child/1/2/3 the returned node will be child/1.
     *
     * @param parentNode The parent node of the path.
     * @param path       The path relative to the parent node we are looking for.
     * @return The deepest node under the parent node for the given path. If no node under the parent matches part of
     *         the path, the parent path is returned.
     */
    private ActionableItemTreeNode getNodeAt(ActionableItemTreeNode parentNode, String path) {
        String firstPart = PathUtils.getPathFirstPart(path);
        if (firstPart.length() > 0) {
            Enumeration children = parentNode.children();
            while (children.hasMoreElements()) {
                ActionableItemTreeNode child = (ActionableItemTreeNode) children.nextElement();
                RepoAwareActionableItem childItem = (RepoAwareActionableItem) child.getUserObject();
                RepoPath childRepoPath = childItem.getRepoPath();
                String name = PathUtils.getName(getTreePath(childRepoPath));
                if (name.equals(firstPart)) {
                    //Handle compacted folders
                    String displayName = child.getUserObject().getDisplayName();
                    int from = path.indexOf(displayName) + displayName.length() + 1;
                    String newPath = from < path.length() ? path.substring(from) : "";
                    return getNodeAt(child, newPath);
                }
            }
        }
        return parentNode;
    }

    private static String getTreePath(RepoPath repoPath) {
        return repoPath.getRepoKey() + "/" + repoPath.getPath();
    }

    private static void setChildren(ActionableItemTreeNode node,
                                    List<? extends ActionableItem> children) {
        node.removeAllChildren();
        for (ActionableItem child : children) {
            ActionableItemTreeNode newChildNode = new ActionableItemTreeNode(child);
            node.add(newChildNode);
        }
    }

    private static void debugExpand(HierarchicActionableItem item, boolean start) {
        if (!log.isDebugEnabled()) {
            return;
        }
        if (item instanceof RepoAwareActionableItem) {
            RepoAwareActionableItem raai = (RepoAwareActionableItem) item;
            RepoPath repoPath = raai.getRepoPath();
            if (start) {
                log.debug("Expanding tree node '" + repoPath + "'...");
            } else {
                log.debug("Expanded tree node '" + repoPath + "'.");
            }
        }
    }
}
