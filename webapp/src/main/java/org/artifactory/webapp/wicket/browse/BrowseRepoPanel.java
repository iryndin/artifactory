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

package org.artifactory.webapp.wicket.browse;

import org.apache.log4j.Logger;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.ItemActionEvent;
import org.artifactory.webapp.actionable.model.ActionDescriptor;
import static org.artifactory.webapp.actionable.model.ActionDescriptor.*;
import org.artifactory.webapp.actionable.model.ActionDescriptorTarget;
import org.artifactory.webapp.actionable.model.ActionableItem;
import org.artifactory.webapp.actionable.model.GlobalRepoActionableItem;
import org.artifactory.webapp.actionable.model.HierarchicActionableItem;
import org.artifactory.webapp.wicket.component.panel.feedback.FeedbackEnabledPanel;
import org.artifactory.webapp.wicket.component.tree.ActionableItemTreeNode;
import org.artifactory.webapp.wicket.component.tree.ActionableItemsProvider;
import org.artifactory.webapp.wicket.component.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.component.tree.NodeActionListener;

import java.util.Arrays;
import java.util.List;

/**
 * Note: this class in not thread safe!
 *
 * @author Yoav Landman
 */
public class BrowseRepoPanel extends FeedbackEnabledPanel implements ActionableItemsProvider {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BrowseRepoPanel.class);

    private final ActionableItemsTree tree;
    private final ModalWindow textContentViewer;
    private Panel nodeDisplayPanel;

    @SpringBean
    private AuthorizationService authService;

    public BrowseRepoPanel(String string) {
        super(string);
        //Add the tree (initial state)
        tree = new ActionableItemsTree("tree", this);
        add(tree);
        nodeDisplayPanel = new EmptyPanel("nodePanel");
        nodeDisplayPanel.setOutputMarkupId(true);
        add(nodeDisplayPanel);
        textContentViewer = new ModalWindow("contentDialog");
        add(textContentViewer);
    }

    public HierarchicActionableItem getRoot() {
        return new GlobalRepoActionableItem();
    }

    public List<? extends ActionableItem> getChildren(HierarchicActionableItem parent) {
        List<? extends ActionableItem> children = parent.getChildren(authService);
        //Filter out candidates thatr are not clearing up
        for (ActionableItem item : children) {
            item.filterActions(authService);
        }
        return children;
    }

    public boolean hasChildren(HierarchicActionableItem parent) {
        return parent.hasChildren(authService);
    }

    public List<ActionDescriptorTarget> getActionDescriptorTargets() {
        return Arrays.asList(new ActionDescriptorTarget(DOWNLOAD),
                new ActionDescriptorTarget(VIEW, textContentViewer),
                new ActionDescriptorTarget(REMOVE, this, "Are you sure you wish to remove"),
                new ActionDescriptorTarget(ZAP, this));
    }

    public NodeActionListener getNodeActionListener() {
        return new RepoNodeActionListener();
    }

    public Panel getNodeDisplayPanel() {
        return nodeDisplayPanel;
    }

    public void setNodeDisplayPanel(Panel panel) {
        this.nodeDisplayPanel = panel;
    }

    private class RepoNodeActionListener implements NodeActionListener {

        public void nodeActionPerformed(ActionableItemTreeNode node, ItemActionEvent event) {
            ActionDescriptor descriptor = event.getDescriptor();
            switch (descriptor) {
                case REMOVE:
                    ActionableItemTreeNode parentNode = node.getParent();
                    if (parentNode != null) {
                        node.removeFromParent();
                        ITreeState state = tree.getTreeState();
                        state.expandNode(parentNode);
                        break;
                    }
                default:
            }
        }

    }
}
