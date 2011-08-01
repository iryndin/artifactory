/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.wicket.panel.tabbed.tab;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.addon.wicket.disabledaddon.DisableLinkBehavior;

/**
 * Tab which is disabled.
 *
 * @author Noam Y. Tenne
 */
public class DisabledTab extends BaseTab {

    public DisabledTab(String title) {
        super(title);
    }

    protected DisabledTab(IModel<String> title) {
        super(title);
    }

    @Override
    public boolean isEnabled() {
        return false;
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
