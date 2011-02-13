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

package org.artifactory.webapp.wicket.page.config.repos.remote;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.addon.wicket.PropertiesWebAddon;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.webapp.wicket.page.config.repos.CachingDescriptorHelper;
import org.artifactory.webapp.wicket.page.config.repos.RepoConfigCreateUpdatePanel;

import java.util.List;

/**
 * Remote repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class HttpRepoPanel extends RepoConfigCreateUpdatePanel<HttpRepoDescriptor> {

    private final CreateUpdateAction action;
    private final HttpRepoDescriptor repoDescriptor;
    private final CachingDescriptorHelper cachingDescriptorHelper;

    public HttpRepoPanel(CreateUpdateAction action, HttpRepoDescriptor repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);
        setWidth(610);
        this.action = action;
        this.repoDescriptor = repoDescriptor;
        this.cachingDescriptorHelper = cachingDescriptorHelper;
    }

    @Override
    protected List<ITab> getConfigurationTabs() {
        List<ITab> tabs = Lists.newArrayList();

        tabs.add(new AbstractTab(Model.<String>of("Basic Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new HttpRepoBasicPanel(panelId, repoDescriptor);
            }
        });

        tabs.add(new AbstractTab(Model.<String>of("Advanced Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new HttpRepoAdvancedPanel(panelId, action, repoDescriptor,
                        cachingDescriptorHelper.getModelMutableDescriptor());
            }
        });

        PropertiesWebAddon propertiesWebAddon = addons.addonByType(PropertiesWebAddon.class);
        List<PropertySet> propertySets = getCachingDescriptorHelper().getModelMutableDescriptor().getPropertySets();
        tabs.add(propertiesWebAddon.getRepoConfigPropertySetsTab("Property Sets", entity, propertySets));

        return tabs;
    }

    @Override
    public void addAndSaveDescriptor(HttpRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        repoDescriptor.setKey(key);
        mccd.addRemoteRepository(repoDescriptor);
        helper.syncAndSaveRemoteRepositories();
    }

    @Override
    public void saveEditDescriptor(HttpRepoDescriptor repoDescriptor) {
        getCachingDescriptorHelper().syncAndSaveRemoteRepositories();
    }

    @Override
    protected boolean validate(HttpRepoDescriptor repoDescriptor) {
        boolean urlValid = StringUtils.isNotEmpty(repoDescriptor.getUrl());
        if (!urlValid) {
            error("Field 'Url' is required.");
        }
        return urlValid;
    }
}
