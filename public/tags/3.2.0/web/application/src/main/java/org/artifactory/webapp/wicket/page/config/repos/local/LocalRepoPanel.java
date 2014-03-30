/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config.repos.local;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.addon.wicket.PropertiesWebAddon;
import org.artifactory.addon.wicket.ReplicationWebAddon;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.page.config.repos.CachingDescriptorHelper;
import org.artifactory.webapp.wicket.page.config.repos.RepoConfigCreateUpdatePanel;
import org.artifactory.webapp.wicket.util.CronUtils;

import java.util.List;
import java.util.Map;

/**
 * Local repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class LocalRepoPanel extends RepoConfigCreateUpdatePanel<LocalRepoDescriptor> {

    private LocalReplicationDescriptor replicationDescriptor;

    public LocalRepoPanel(CreateUpdateAction action, LocalRepoDescriptor repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);
        add(new CssClass("local-repo-config"));
    }

    @Override
    protected List<ITab> getConfigurationTabs() {
        List<ITab> tabList = Lists.newArrayList();

        tabList.add(new AbstractTab(Model.of("Basic Settings")) {

            @Override
            public Panel getPanel(String panelId) {
                return new LocalRepoBasicPanel(panelId, getRepoDescriptor(), isCreate());
            }
        });

        PropertiesWebAddon propertiesWebAddon = addons.addonByType(PropertiesWebAddon.class);
        List<PropertySet> propertySets = getCachingDescriptorHelper().getModelMutableDescriptor().getPropertySets();
        tabList.add(propertiesWebAddon.getRepoConfigPropertySetsTab("Property Sets", entity, propertySets));

        MutableCentralConfigDescriptor mutableDescriptor = cachingDescriptorHelper.getModelMutableDescriptor();
        replicationDescriptor = mutableDescriptor.getLocalReplication(entity.getKey());
        if (replicationDescriptor == null) {
            replicationDescriptor = new LocalReplicationDescriptor();
            replicationDescriptor.setRepoKey(entity.getKey());
        }
        ReplicationWebAddon replicationWebAddon = addons.addonByType(ReplicationWebAddon.class);
        tabList.add(replicationWebAddon.getLocalRepoReplicationPanel("Replication", entity, replicationDescriptor,
                mutableDescriptor, action));

        tabList.add(new AbstractTab(Model.of("Packages")) {
            @Override
            public Panel getPanel(String panelId) {
                return new LocalRepoPackagesPanel(panelId, entity, isCreate());
            }
        });

        return tabList;
    }

    @Override
    public void addAndSaveDescriptor(LocalRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        mccd.addLocalRepository(repoDescriptor);
        if (replicationDescriptor.isEnabled()) {
            if (StringUtils.isBlank(replicationDescriptor.getRepoKey())) {
                replicationDescriptor.setRepoKey(repoDescriptor.getKey());
            }
            mccd.addLocalReplication(replicationDescriptor);
        }
        helper.syncAndSaveLocalRepositories();
    }

    @Override
    public void saveEditDescriptor(LocalRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        //update the model being saved
        Map<String, LocalRepoDescriptor> localRepositoriesMap = mccd.getLocalRepositoriesMap();
        if (localRepositoriesMap.containsKey(repoDescriptor.getKey())) {
            localRepositoriesMap.put(repoDescriptor.getKey(), repoDescriptor);
        }
        if (replicationDescriptor.isEnabled() && !mccd.isLocalReplicationExists(replicationDescriptor)) {
            if (StringUtils.isBlank(replicationDescriptor.getRepoKey())) {
                replicationDescriptor.setRepoKey(repoDescriptor.getKey());
            }
            mccd.addLocalReplication(replicationDescriptor);
        }
        helper.syncAndSaveLocalRepositories();
    }

    @Override
    protected boolean validate(LocalRepoDescriptor repoDescriptor) {
        if (replicationDescriptor != null && replicationDescriptor.isEnabled()) {
            if (StringUtils.isBlank(replicationDescriptor.getUrl())) {
                error("Field 'Url' is required.");
                return false;
            }

            if (StringUtils.isBlank(replicationDescriptor.getUsername())) {
                error("Username is required.");
                return false;
            }

            if (replicationDescriptor.getSocketTimeoutMillis() < 0) {
                error("Socket Timeout must be positive or zero.");
                return false;
            }

            String cronExp = replicationDescriptor.getCronExp();
            if (StringUtils.isBlank(cronExp) || !CronUtils.isValid(cronExp)) {
                error("Invalid cron expression");
                return false;
            }
        }

        return true;
    }
}
