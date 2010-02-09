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

package org.artifactory.webapp.wicket.component.tree;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.actionable.model.ActionableItem;
import org.artifactory.webapp.actionable.model.HierarchicActionableItem;
import org.artifactory.webapp.wicket.component.tree.menu.ActionsMenuPanel;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ActionableItemsTree extends Tree {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(ActionableItemsTree.class);

    private ActionableItemsProvider provider;

    public ActionableItemsTree(String id, ActionableItemsProvider provider) {
        super(id);
        this.provider = provider;
        setRootLess(false);
        HierarchicActionableItem root = provider.getRoot();
        ActionableItemTreeNode rootNode = new ActionableItemTreeNode(root);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        setModel(new Model(treeModel));
        List<? extends ActionableItem> children = provider.getChildren(root);
        setChildren(rootNode, children);
        getTreeState().expandNode(rootNode);
        setOutputMarkupId(true);
    }

    public ActionableItemsProvider getProvider() {
        return provider;
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    protected ResourceReference getNodeIcon(TreeNode node) {
        ActionableItemTreeNode treeNode = (ActionableItemTreeNode) node;
        ActionableItem item = treeNode.getUserObject();
        String res = item.getIconRes();
        return new ResourceReference(res);
    }

    @Override
    protected void populateTreeItem(WebMarkupContainer item, int level) {
        super.populateTreeItem(item, level);
        ActionableItemTreeNode node = (ActionableItemTreeNode) item.getModelObject();
        ActionsMenuPanel actionsMenuPanel =
                new ActionsMenuPanel("actionsMenu", node, this, item);
        item.add(actionsMenuPanel);

    }

    @Override
    protected void onJunctionLinkClicked(AjaxRequestTarget target, TreeNode node) {
        super.onJunctionLinkClicked(target, node);
        boolean expanded = isNodeExpanded(node);
        ActionableItemTreeNode mutableTreeNode = (ActionableItemTreeNode) node;
        HierarchicActionableItem item =
                (HierarchicActionableItem) mutableTreeNode.getUserObject();
        if (expanded) {
            List<? extends ActionableItem> children = provider.getChildren(item);
            setChildren(mutableTreeNode, children);
        }
        target.appendJavascript("hideDisplayPanel(); setTimeout(fixDisplayPanel, 100);");
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
        super.onNodeLinkClicked(target, node);
        ActionableItemTreeNode mutableTreeNode = (ActionableItemTreeNode) node;
        ActionableItem item = mutableTreeNode.getUserObject();
        Panel oldDisplayPanel = provider.getNodeDisplayPanel();
        Panel dispalyPanel = item.createDispalyPanel(oldDisplayPanel.getId());
        dispalyPanel.setOutputMarkupId(true);
        oldDisplayPanel.replaceWith(dispalyPanel);
        target.addComponent(dispalyPanel);
        provider.setNodeDisplayPanel(dispalyPanel);
        target.appendJavascript("setTimeout(fixDisplayPanel, 100);");
        getTreeState().selectNode(node, true);
    }

    @Override
    protected Component newJunctionLink(MarkupContainer parent, String id, String imageId,
            TreeNode node) {
        //Collapse empty nodes
        ActionableItemTreeNode mutableTreeNode = (ActionableItemTreeNode) node;
        ActionableItem userObject = mutableTreeNode.getUserObject();
        if (userObject instanceof HierarchicActionableItem) {
            boolean hasChildren = provider.hasChildren((HierarchicActionableItem) userObject);
            //Must be set before the call to super
            mutableTreeNode.setLeaf(!hasChildren);
        } else {
            mutableTreeNode.setLeaf(true);
        }
        return super.newJunctionLink(parent, id, imageId, node);
    }

    private static void setChildren(ActionableItemTreeNode node,
            List<? extends ActionableItem> children) {
        node.removeAllChildren();
        for (ActionableItem child : children) {
            ActionableItemTreeNode newChildNode = new ActionableItemTreeNode(child);
            node.add(newChildNode);
        }
    }
}
