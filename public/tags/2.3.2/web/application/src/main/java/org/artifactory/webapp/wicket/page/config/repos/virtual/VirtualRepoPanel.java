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

package org.artifactory.webapp.wicket.page.config.repos.virtual;

import com.google.common.collect.Lists;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.webapp.wicket.page.config.repos.CachingDescriptorHelper;
import org.artifactory.webapp.wicket.page.config.repos.RepoConfigCreateUpdatePanel;

import java.util.List;
import java.util.Map;

/**
 * Virtual repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class VirtualRepoPanel extends RepoConfigCreateUpdatePanel<VirtualRepoDescriptor> {

    private final CreateUpdateAction action;
    private final VirtualRepoDescriptor repoDescriptor;
    private final CachingDescriptorHelper cachingDescriptorHelper;

    public VirtualRepoPanel(CreateUpdateAction action, VirtualRepoDescriptor repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);
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
                return new VirtualRepoBasicPanel(panelId, action, repoDescriptor, cachingDescriptorHelper);
            }
        });

        tabs.add(new AbstractTab(Model.<String>of("Advanced Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new VirtualRepoAdvancedPanel(panelId, action, repoDescriptor, form);
            }
        });

        return tabs;
    }

    @Override
    public void addAndSaveDescriptor(VirtualRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        repoDescriptor.setKey(key);
        mccd.addVirtualRepository(repoDescriptor);
        helper.syncAndSaveVirtualRepositories();
    }

    @Override
    public void saveEditDescriptor(VirtualRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        //update the model being saved
        Map<String, VirtualRepoDescriptor> virtualRepos =
                helper.getModelMutableDescriptor().getVirtualRepositoriesMap();
        if (virtualRepos.containsKey(repoDescriptor.getKey())) {
            virtualRepos.put(repoDescriptor.getKey(), repoDescriptor);
        }
        helper.syncAndSaveVirtualRepositories();
    }

    @Override
    protected boolean validate(VirtualRepoDescriptor repoDescriptor) {
        return true;
    }
}
