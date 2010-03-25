/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.wicket.actionable.link.ActionLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This panel displays action buttons available for an item (file or folder) in the tree browser.
 *
 * @author Yossi Shaul
 */
public class ActionsPanel extends Panel {
    public ActionsPanel(String id, ActionableItem repoItem) {
        super(id);
        addActions(repoItem);
    }

    private void addActions(final ActionableItem repoItem) {
        Set<ItemAction> actions = repoItem.getActions();
        final List<ItemAction> actionList = new ArrayList<ItemAction>(actions.size());
        //Filter enabled actions
        for (ItemAction action : actions) {
            if (action.isEnabled()) {
                actionList.add(action);
            }
        }

        FieldSetBorder actionBorder = new FieldSetBorder("actionBorder") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && !actionList.isEmpty();
            }
        };
        add(actionBorder);

        actionBorder.add(new ListView("action", actionList) {
            @Override
            protected void populateItem(ListItem item) {
                ItemAction action = (ItemAction) item.getModelObject();
                item.add(new ActionLink("link", action, repoItem));
            }
        });
    }

}
