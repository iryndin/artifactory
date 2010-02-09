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

package org.artifactory.webapp.wicket.common.component.tree.menu;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.RepoAwareItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.tree.ActionableItemTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A panel that contains the popup menu actions
 *
 * @author Yoav Landman
 */
public class ActionsMenuPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ActionsMenuPanel.class);

    public ActionsMenuPanel(String id, final ActionableItemTreeNode node) {
        super(id, new Model(node));
        setOutputMarkupId(true);
        //Render the enabled actions for each node
        final ActionableItem actionableItem = node.getUserObject();
        Set<ItemAction> actions = actionableItem.getContextMenuActions();
        List<ItemAction> menuActions = new ArrayList<ItemAction>(actions.size());
        //Filter non-menu actions
        for (ItemAction action : actions) {
            if (action.isEnabled()) {
                menuActions.add(action);
            }
        }
        ListView menuItems = new ListView("menuItem", menuActions) {
            @Override
            protected void populateItem(ListItem item) {
                final ItemAction action = (ItemAction) item.getModelObject();
                final boolean disabled = !action.isEnabled();
                if (disabled) {
                    item.add(new AttributeModifier("disabled", true, new Model("true")));
                }
                Label label = new Label("label", action.getName());
                label.add(new CssClass(action.getCssClass()));
                item.add(label);
                item.add(new AjaxEventBehavior("onClick") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        ItemEvent event;
                        if (actionableItem instanceof RepoAwareActionableItem) {
                            event = new RepoAwareItemEvent(
                                    (RepoAwareActionableItem) actionableItem,
                                    (RepoAwareItemAction) action, target);
                        } else {
                            event = new ItemEvent(actionableItem, action, target);
                        }
                        //Double-check security in a generic way
                        if (disabled) {
                            log.warn("Tried to execute a disabled action: " + event.toString());
                            return;
                        }
                        try {
                            action.actionPerformed(event);
                        } catch (Exception e) {
                            error(e);
                            FeedbackUtils.refreshFeedback(target);
                            log.error("Failed to process tree event.", e);
                        }
                    }

                    /**
                     * Display a confirmation dialog for an action
                     * @return
                     */
                    @Override
                    protected CharSequence getCallbackScript() {
                        CharSequence orig = super.getCallbackScript();
                        return action.getConfirmationCallbackScript(actionableItem, orig);
                    }
                });
            }
        };
        WebMarkupContainer menu = new WebMarkupContainer("menu");
        menu.setMarkupId("contextMenu");
        menu.setOutputMarkupId(true);
        menu.add(menuItems);
        add(menu);
    }
}
