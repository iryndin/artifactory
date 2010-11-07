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

package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.Page;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WebApplicationAddon;
import org.artifactory.common.wicket.panel.logo.BaseLogoPanel;

/**
 * @author Tomer Cohen
 */
public class HeaderLogoPanel extends BaseLogoPanel {
    @SpringBean
    private AddonsManager addons;

    public HeaderLogoPanel(String id) {
        super(id);
    }

    @Override
    protected Class<? extends Page> getLinkPage() {
        WebApplicationAddon applicationAddon = addons.addonByType(WebApplicationAddon.class);
        return applicationAddon.getHomePage();
    }

    @Override
    protected String getLogoUrl() {
        WebApplicationAddon applicationAddon = addons.addonByType(WebApplicationAddon.class);
        return applicationAddon.getCompanyLogoUrl();
    }
}

