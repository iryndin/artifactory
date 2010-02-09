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

package org.artifactory.webapp.wicket.panel.tabbed;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonTab;
import org.artifactory.common.wicket.util.CookieUtils;

import java.util.List;

/**
 * A StyledTabbedPanel that remembers the last selected tab.
 *
 * @author Yossi Shaul
 */
public class PersistentTabbedPanel extends StyledTabbedPanel {
    private static final String COOKIE_NAME = "browse-last-tab";
    private static final int UNSET = -1;

    private int lastTabIndex;

    public PersistentTabbedPanel(String id, List<ITab> tabs) {
        super(id, tabs);

        lastTabIndex = getLastTabIndex();
    }

    @Override
    protected void onBeforeRender() {
        selectLastTab();
        super.onBeforeRender();
    }

    /**
     * Re-select last selected tab. To be called before render becuase some tabs are loaded lazily (calling
     * selectLastTab from c'tor might cause NPE).
     */
    private void selectLastTab() {
        if (lastTabIndex != UNSET) {
            setSelectedTab(lastTabIndex);
            lastTabIndex = UNSET;
        }
    }

    /**
     * Return last tab index as stored in cookie.
     */
    @SuppressWarnings({"unchecked"})
    private int getLastTabIndex() {
        List<ITab> tabs = getTabs();
        String lastTabName = CookieUtils.getCookie(COOKIE_NAME);
        if (lastTabName != null) {
            for (int i = 0; i < tabs.size(); i++) {
                ITab tab = tabs.get(i);
                String tabName = tab.getTitle().getObject().toString();
                if (tabName.equals(lastTabName) && !(tab instanceof DisabledAddonTab)) {
                    return i;
                }
            }
        }
        return UNSET;
    }

    @Override
    protected void onAjaxUpdate(AjaxRequestTarget target) {
        super.onAjaxUpdate(target);

        // store last tab name in a cookie
        ITab tab = (ITab) getTabs().get(getSelectedTab());
        CookieUtils.setCookie(COOKIE_NAME, tab.getTitle().getObject().toString());
    }
}
