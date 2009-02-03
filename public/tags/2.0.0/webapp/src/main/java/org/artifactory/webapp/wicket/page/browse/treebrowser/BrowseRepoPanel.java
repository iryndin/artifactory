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

package org.artifactory.webapp.wicket.page.browse.treebrowser;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ItemActionListener;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.actionable.model.GlobalRepoActionableItem;
import org.artifactory.webapp.actionable.model.HierarchicActionableItem;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.panel.shortcutkey.KeyEventHandler;
import org.artifactory.webapp.wicket.common.component.panel.shortcutkey.KeyListener;
import org.artifactory.webapp.wicket.common.component.panel.shortcutkey.KeyReleasedEvent;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.common.component.tree.ActionableItemTreeNode;
import org.artifactory.webapp.wicket.common.component.tree.ActionableItemsProvider;
import org.artifactory.webapp.wicket.common.component.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.common.component.tree.menu.ActionsMenuPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import static java.awt.event.KeyEvent.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Note: this class in not thread safe!
 *
 * @author Yoav Landman
 */
public class BrowseRepoPanel extends TitledPanel implements ActionableItemsProvider, ItemActionListener {

    /**
     * Wicket container for the tabs panel
     */
    private final WebMarkupContainer nodePanelContainer;

    /**
     * Selected node information will be displayed in this panel (the tabs)
     */
    private Panel nodeDisplayPanel;

    /**
     * A modal window for displaying text  content
     */
    private final ModalHandler textContentViewer;

    private final ActionableItemsTree tree;

    @SpringBean
    private AuthorizationService authService;

    public BrowseRepoPanel(String id) {
        this(id, null);
    }

    public BrowseRepoPanel(String id, ActionableItem initialItem) {
        super(id);

        WebMarkupContainer menuPlaceHolder = new WebMarkupContainer("contextMenu");
        menuPlaceHolder.setOutputMarkupId(true);
        add(menuPlaceHolder);

        nodePanelContainer = new WebMarkupContainer("nodePanelContainer");
        nodePanelContainer.setOutputMarkupId(true);
        add(nodePanelContainer);

        nodeDisplayPanel = new EmptyPanel("nodePanel");
        nodeDisplayPanel.setOutputMarkupId(true);
        nodePanelContainer.add(nodeDisplayPanel);

        textContentViewer = new ModalHandler("contentDialog");
        add(textContentViewer);

        RepoPath repoPath = null;
        if (initialItem instanceof RepoAwareActionableItem) {
            repoPath = ((RepoAwareActionableItem) initialItem).getRepoPath();
        }

        tree = new ActionableItemsTree("tree", this, repoPath) {
            @Override
            protected void onContextMenu(WebMarkupContainer item, AjaxRequestTarget target) {
                super.onContextMenu(item, target);
                ActionableItemTreeNode node = (ActionableItemTreeNode) item.getModelObject();

                Set<ItemAction> actions = node.getUserObject().getContextMenuActions();
                // check as least one action is enabled
                for (ItemAction action : actions) {
                    if (action.isEnabled()) {
                        // show context menu
                        ActionsMenuPanel menuPanel = new ActionsMenuPanel("contextMenu", node);
                        BrowseRepoPanel.this.replace(menuPanel);
                        target.addComponent(menuPanel);
                        target.appendJavascript("ActionsMenuPanel.show();");
                        return;
                    }
                }
            }
        };
        add(tree);

        addKeyEventHandler();
    }

    private void addKeyEventHandler() {
        KeyEventHandler keyEventHandler = new KeyEventHandler("keyEventHandler");
        keyEventHandler.setVisible(false);
        add(keyEventHandler);

        keyEventHandler.addKeyListener(new KeyListener() {
            public void keyReleased(KeyReleasedEvent e) {
                TreeNode selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    return;
                }

                ITreeState state = tree.getTreeState();
                boolean expanded = state.isNodeExpanded(selectedNode);
                if (!expanded && !selectedNode.isLeaf() && selectedNode.getAllowsChildren()) {
                    state.expandNode(selectedNode);
                    tree.onJunctionLinkClicked(e.getTarget(), selectedNode);
                    e.getTarget().addComponent(tree);
                } else {
                    selectNode(selectedNode, getNextTreeNode(selectedNode), e.getTarget());
                }
            }
        }, VK_RIGHT, VK_ADD);

        keyEventHandler.addKeyListener(new KeyListener() {
            public void keyReleased(KeyReleasedEvent e) {
                TreeNode selectedNode = getSelectedNode();
                if (selectedNode == null) {
                    return;
                }

                ITreeState state = tree.getTreeState();
                boolean expanded = state.isNodeExpanded(selectedNode);
                if (expanded) {
                    state.collapseNode(selectedNode);
                    tree.onJunctionLinkClicked(e.getTarget(), selectedNode);
                    e.getTarget().addComponent(tree);
                } else {
                    selectNode(selectedNode, getPrevTreeNode(selectedNode), e.getTarget());
                }
            }
        }, VK_LEFT, VK_SUBTRACT);

        keyEventHandler.addKeyListener(new KeyListener() {
            public void keyReleased(KeyReleasedEvent e) {
                TreeNode selectedNode = getSelectedNode();
                if (selectedNode != null) {
                    selectNode(selectedNode, getPrevTreeNode(selectedNode), e.getTarget());
                }

            }
        }, VK_UP);

        keyEventHandler.addKeyListener(new KeyListener() {
            public void keyReleased(KeyReleasedEvent e) {
                TreeNode selectedNode = getSelectedNode();
                if (selectedNode != null) {
                    selectNode(selectedNode, getNextTreeNode(selectedNode), e.getTarget());
                }
            }
        }, VK_DOWN);
    }

    private void selectNode(TreeNode selectedNode, TreeNode newSelection, AjaxRequestTarget target) {
        ITreeState state = tree.getTreeState();
        if (selectedNode != null && newSelection != null) {
            state.selectNode(selectedNode, false);
            state.selectNode(newSelection, true);
            target.addComponent(tree);
        }
    }

    private TreeNode getNextTreeNode(TreeNode node) {
        ITreeState state = tree.getTreeState();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) {
            return null;
        }
        if (!node.isLeaf() && node.getAllowsChildren() && state.isNodeExpanded(node)) {
            return node.getChildAt(0);
        }

        TreeNode nextNode = parent.getChildAfter(node);
        if (nextNode == null) {
            return getNextParent(parent);
        }
        return nextNode;
    }

    private TreeNode getNextParent(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) {
            return null;
        }
        TreeNode nextNode = parent.getChildAfter(node);
        if (nextNode == null) {
            return getNextParent(parent);
        }
        return nextNode;
    }

    private TreeNode getPrevTreeNode(TreeNode node) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) {
            return null;
        }

        TreeNode prevNode = parent.getChildBefore(node);
        if (prevNode != null) {
            return prevNode;
        }

        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModelObject();
        if (parent == treeModel.getRoot()) {
            return null;
        }
        return parent;
    }

    @SuppressWarnings({"unchecked"})
    public TreeNode getSelectedNode() {
        Collection<TreeNode> selectedNodes = tree.getTreeState().getSelectedNodes();
        if (selectedNodes.isEmpty()) {
            return null;
        }
        return selectedNodes.iterator().next();
    }

    public HierarchicActionableItem getRoot() {
        return new GlobalRepoActionableItem();
    }

    public List<? extends ActionableItem> getChildren(HierarchicActionableItem parent) {
        List<? extends ActionableItem> children = parent.getChildren(authService);
        for (ActionableItem item : children) {
            // Add the tree as a listener (required for remove)
            // Must delegate to tree, because tree might be null at this point
            item.addActionListener(this);

            // Add the event targets
            ItemEventTargetComponents targetComponents =
                    new ItemEventTargetComponents() {
                        @Override
                        public Component getRefreshableComponent() {
                            return tree;
                        }

                        @Override
                        public WebMarkupContainer getNodePanelContainer() {
                            return BrowseRepoPanel.this.nodePanelContainer;
                        }

                        @Override
                        public ModalWindow getModalWindow() {
                            return textContentViewer;
                        }
                    };
            item.setEventTargetComponents(targetComponents);

            // Filter out candidates that are not clearing up
            item.filterActions(authService);
        }
        return children;
    }

    public boolean hasChildren(HierarchicActionableItem parent) {
        return parent.hasChildren(authService);
    }

    public Panel getItemDisplayPanel() {
        return nodeDisplayPanel;
    }

    public void setItemDisplayPanel(Panel panel) {
        nodeDisplayPanel = panel;
    }

    public void actionPerformed(ItemEvent e) {
        tree.actionPerformed(e);
    }
}
