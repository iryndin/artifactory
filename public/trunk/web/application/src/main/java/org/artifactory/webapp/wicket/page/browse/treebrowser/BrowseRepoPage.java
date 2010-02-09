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

package org.artifactory.webapp.wicket.page.browse.treebrowser;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

import java.io.Serializable;

public class BrowseRepoPage extends AuthenticatedPage implements Serializable {
    private String lastTabName;

    public BrowseRepoPage() {
        this(null);
    }

    public BrowseRepoPage(ActionableItem initialItem) {
        add(new BrowseRepoPanel("browseRepoPanel", initialItem));

        WebMarkupContainer scrollScript = new WebMarkupContainer("scrollScript");
        scrollScript.setVisible(initialItem != null);
        add(scrollScript);
    }

    @Override
    public String getPageName() {
        return "Repository Browser";
    }

    public String getLastTabName() {
        return lastTabName;
    }

    public void setLastTabName(String lastTabName) {
        this.lastTabName = lastTabName;
    }
}
