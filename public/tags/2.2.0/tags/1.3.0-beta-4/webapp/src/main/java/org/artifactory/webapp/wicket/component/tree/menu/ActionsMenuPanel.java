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

package org.artifactory.webapp.wicket.component.tree.menu;

import org.apache.commons.collections15.OrderedMap;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.actionable.ItemAction;
import org.artifactory.webapp.actionable.ItemActionEvent;
import org.artifactory.webapp.actionable.model.ActionDescriptor;
import org.artifactory.webapp.actionable.model.ActionDescriptorTarget;
import org.artifactory.webapp.actionable.model.ActionableItem;
import org.artifactory.webapp.wicket.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.component.tree.ActionableItemTreeNode;
import org.artifactory.webapp.wicket.component.tree.ActionableItemsProvider;
import org.artifactory.webapp.wicket.component.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.component.tree.NodeActionListener;

import java.util.List;

/**
 * A panel that contains the popup menu actions
 *
 * @author Yoav Landman
 */
public class ActionsMenuPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(ActionsMenuPanel.class);

    public ActionsMenuPanel(String id, final ActionableItemTreeNode node, ActionableItemsTree tree) {
        super(id, new Model(node));
        setOutputMarkupId(true);

        WebMarkupContainer menu = new WebMarkupContainer("menu");
        menu.setMarkupId("contextMenu");
        menu.setOutputMarkupId(true);
        add(menu);
        //Get the popup menu actions from the provider and render the enabled ones for each node
        final ActionableItemsProvider itemsProvider = tree.getProvider();
        List<ActionDescriptorTarget> descriptorTargets =
                itemsProvider.getActionDescriptorTargets();
        ListView menuItems = new ListView("menuItem", descriptorTargets) {
            @Override
            protected void populateItem(ListItem item) {
                final ActionDescriptorTarget descriptorTarget =
                        (ActionDescriptorTarget) item.getModelObject();
                //Check if enabled
                final ActionableItem actionableItem = node.getUserObject();
                OrderedMap<ActionDescriptor, ItemAction> actions = actionableItem.getActions();
                final ActionDescriptor descriptor = descriptorTarget.getDescriptor();
                final ItemAction action = actions.get(descriptor);
                final boolean disabled = action == null || !action.isEnabled();
                if (disabled) {
                    item.add(new AttributeModifier("disabled", true, new Model("true")));
                }
                Image icon =
                        new Image("icon", new ResourceReference(descriptor.getIconRes()));
                item.add(icon);
                item.add(new Label("label", descriptor.getName()));
                item.add(new AjaxEventBehavior("onClick") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        List<Component> targetComponents = descriptorTarget.getTargets();
                        ItemActionEvent event =
                                new ItemActionEvent(
                                        actionableItem, descriptor, targetComponents, target);
                        //Double-check security in a generic way
                        if (disabled) {
                            LOGGER.warn("Tried to execute a disabled action: " + event);
                            return;
                        }
                        try {
                            action.actionPerformed(event);
                            //Notify the tree
                            NodeActionListener nodeActionListener =
                                    itemsProvider.getNodeActionListener();
                            nodeActionListener.nodeActionPerformed(node, event);
                        } catch (Exception e) {
                            error(e);
                            FeedbackUtils.refreshFeedback(target);
                            LOGGER.error("Failed to process tree event.", e);
                        }
                        //Automatically add the target component to the response
                        if (targetComponents != null) {
                            for (Component component : targetComponents) {
                                target.addComponent(component);
                            }
                        }
                    }

                    @Override
                    protected CharSequence getCallbackScript() {
                        CharSequence confirmationPrefix = descriptorTarget.getActionConfirmationPrefix();

                        if (confirmationPrefix != null) {
                            CharSequence orig = super.getCallbackScript();

                            return "if (confirm('" + confirmationPrefix + " " +
                                    actionableItem.getDisplayName() + "?')) {" +
                                    orig + "} else { return false; }";
                        }

                        return super.getCallbackScript();
                    }
                });
            }
        };
        menu.add(menuItems);
    }
}
