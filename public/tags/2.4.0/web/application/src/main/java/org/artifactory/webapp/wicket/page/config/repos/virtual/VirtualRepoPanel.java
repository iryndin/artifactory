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
import org.artifactory.addon.p2.P2RemoteRepository;
import org.artifactory.addon.p2.P2RemoteRepositoryModel;
import org.artifactory.addon.p2.P2WebAddon;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.util.CollectionUtils;
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

    public VirtualRepoPanel(CreateUpdateAction action, VirtualRepoDescriptor repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);
        this.action = action;
    }

    @Override
    protected List<ITab> getConfigurationTabs() {
        List<ITab> tabs = Lists.newArrayList();

        tabs.add(new AbstractTab(Model.<String>of("Basic Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new VirtualRepoBasicPanel(panelId, action, getRepoDescriptor(), getCachingDescriptorHelper());
            }
        });

        tabs.add(new AbstractTab(Model.<String>of("Advanced Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new VirtualRepoAdvancedPanel(panelId, action, getRepoDescriptor(), form);
            }
        });

        tabs.add(addons.addonByType(P2WebAddon.class).getVirtualRepoConfigurationTab(
                "P2", getRepoDescriptor(), getCachingDescriptorHelper()));
        return tabs;
    }

    @Override
    public void addAndSaveDescriptor(VirtualRepoDescriptor virtualRepo) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        virtualRepo.setKey(key);
        mccd.addVirtualRepository(virtualRepo);

        boolean updateRemotes = processP2Configuration(virtualRepo, helper);

        helper.syncAndSaveVirtualRepositories(updateRemotes);
    }

    private boolean processP2Configuration(VirtualRepoDescriptor virtualRepo, CachingDescriptorHelper helper) {
        if (!virtualRepo.isP2Support()) {
            return false;
        }
        // go over the p2 remote repos if any and perform required action
        boolean updateRemotes = false;
        List<P2RemoteRepositoryModel> p2RemoteRepos = helper.getP2RemoteRepositoryModels();
        if (CollectionUtils.notNullOrEmpty(p2RemoteRepos)) {
            for (P2RemoteRepositoryModel p2RemoteRepo : p2RemoteRepos) {
                // only perform action if the action checkbox is selected
                if (p2RemoteRepo.isSelected()) {
                    P2RemoteRepository p2RemoteRepository = p2RemoteRepo.p2RemoteRepository;
                    RemoteRepoDescriptor remoteRepoDescriptor = p2RemoteRepository.descriptor;
                    if (p2RemoteRepository.toCreate) {
                        // add new remote repository
                        helper.getModelMutableDescriptor().addRemoteRepository(remoteRepoDescriptor);
                        updateRemotes = true;
                    } else if (p2RemoteRepository.modified) {
                        // replace remote repository configuration
                        remoteRepoDescriptor.setP2Support(true);
                        helper.getModelMutableDescriptor().getRemoteRepositoriesMap().put(
                                remoteRepoDescriptor.getKey(), remoteRepoDescriptor);
                        updateRemotes = true;
                    }

                    if (!p2RemoteRepository.alreadyIncluded) {
                        // add the remote repository to the aggregation list of the virtual
                        virtualRepo.getRepositories().add(remoteRepoDescriptor);
                    }
                }
            }
        }
        return updateRemotes;
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

        boolean updateRemotes = processP2Configuration(repoDescriptor, helper);

        helper.syncAndSaveVirtualRepositories(updateRemotes);
    }

    @Override
    protected boolean validate(VirtualRepoDescriptor repoDescriptor) {
        return true;
    }
}
