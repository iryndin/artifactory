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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

import java.util.concurrent.TimeUnit;

/**
 * Displays online status of remote repository in the general config panel when a cache repo is selected.
 *
 * @author Yossi Shaul
 */
public class OnlineStatusPanel extends Panel {

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private AuthorizationService authorizationService;

    public OnlineStatusPanel(String id, RemoteRepoDescriptor remoteRepo) {
        super(id);
        setOutputMarkupId(true);
        addOnlineInfo(remoteRepo);
    }

    private void addOnlineInfo(final RemoteRepoDescriptor remoteRepo) {
        final boolean isOffline = remoteRepo == null || remoteRepo.isOffline()
                || centralConfigService.getDescriptor().isOfflineMode();
        String status = getStatusText(remoteRepo, isOffline);
        final LabeledValue offlineLabel = new LabeledValue("status", "Online Status: ", status);
        add(offlineLabel);

        WebMarkupContainer resetButton = new TitledAjaxLink("resetButton", "Set Online") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                repositoryService.resetAssumedOffline(remoteRepo.getKey());
                offlineLabel.setValue(getStatusText(remoteRepo, isOffline));
                target.add(OnlineStatusPanel.this);
            }

            @Override
            public boolean isVisible() {
                return authorizationService.isAdmin() &&
                        repositoryService.isRemoteAssumedOffline(remoteRepo.getKey());
            }
        };

        add(resetButton);
    }

    private String getStatusText(RemoteRepoDescriptor remoteRepo, boolean offline) {
        String status = "Online";
        if (offline) {
            status = "Offline";
        } else {
            if (repositoryService.isRemoteAssumedOffline(remoteRepo.getKey())) {
                long nextCheckTime = repositoryService.getRemoteNextOnlineCheck(remoteRepo.getKey());
                long nextCheckSeconds = Math.max(0,
                        TimeUnit.MILLISECONDS.toSeconds(nextCheckTime - System.currentTimeMillis()));
                status = "Assumed offline (retry in " + nextCheckSeconds + " seconds...)";

            }
        }
        return status;
    }
}
