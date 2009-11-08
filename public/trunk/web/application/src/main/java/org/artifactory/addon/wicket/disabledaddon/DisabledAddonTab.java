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

package org.artifactory.addon.wicket.disabledaddon;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.addon.wicket.Addon;
import org.artifactory.webapp.wicket.panel.tabbed.tab.BaseTab;

/**
 * Tab which is disabled by default
 *
 * @author Noam Tenne
 */
public class DisabledAddonTab extends BaseTab {
    private Addon addon;

    public DisabledAddonTab(IModel title, Addon addon) {
        super(title);
        this.addon = addon;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void onNewTabItem(Loop.LoopItem item) {
        super.onNewTabItem(item);
        item.add(new DisabledAddonBehavior(addon));
    }

    @Override
    public void onNewTabLink(Component link) {
        super.onNewTabLink(link);
        link.add(new DisableLinkBehavior());
    }

    @Override
    public Panel getPanel(String panelId) {
        return null;
    }
}
