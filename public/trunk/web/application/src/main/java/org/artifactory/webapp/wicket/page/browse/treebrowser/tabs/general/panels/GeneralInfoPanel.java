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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.actionable.model.LocalRepoActionableItem;

/**
 * Displays general item information. Placed inside the general info panel.
 *
 * @author Yossi Shaul
 */
public class GeneralInfoPanel extends Panel {

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    public GeneralInfoPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        addGeneralInfo(repoItem);
    }

    private void addGeneralInfo(RepoAwareActionableItem repoItem) {
        final boolean itemIsRepo = repoItem instanceof LocalRepoActionableItem;
        LocalRepoDescriptor repoDescriptor = repoItem.getRepo();
        final boolean isCache = repoDescriptor.isCache();
        RemoteRepoDescriptor remoteRepo = null;
        if (isCache) {
            remoteRepo = ((LocalCacheRepoDescriptor) repoDescriptor).getRemoteRepo();
        }

        FieldSetBorder infoBorder = new FieldSetBorder("infoBorder");
        add(infoBorder);

        String itemDisplayName = repoItem.getDisplayName();
        LabeledValue nameLabel = new LabeledValue("name", "Name: ", itemDisplayName);
        infoBorder.add(nameLabel);

        LabeledValue descriptionLabel = new LabeledValue("description", "Description: ");
        String description = null;
        if (itemIsRepo) {
            if (isCache) {
                description = remoteRepo.getDescription();
            } else {
                description = repoDescriptor.getDescription();
            }
            descriptionLabel.setValue(description);
        }
        descriptionLabel.setVisible(!StringUtils.isEmpty(description) && itemIsRepo);
        infoBorder.add(descriptionLabel);

        ItemInfo itemInfo = repoItem.getItemInfo();

        LabeledValue sizeLabel = new LabeledValue("size", "Size: ");
        infoBorder.add(sizeLabel);

        LabeledValue ageLabel = new LabeledValue("age", "Age: ");
        infoBorder.add(ageLabel);

        //Hack
        LabeledValue groupIdLabel = new LabeledValue("groupId", "GroupId: ");
        infoBorder.add(groupIdLabel);

        LabeledValue artifactIdLabel = new LabeledValue("artifactId", "ArtifactId: ");
        infoBorder.add(artifactIdLabel);


        LabeledValue versionLabel = new LabeledValue("version", "Version: ");
        infoBorder.add(versionLabel);

        final boolean isItemARepo = (repoItem instanceof LocalRepoActionableItem);
        LabeledValue deployedByLabel = new LabeledValue("deployed-by", "Deployed by: ",
                itemInfo.getAdditionalInfo().getModifiedBy()) {
            @Override
            public boolean isVisible() {
                return !isItemARepo;
            }
        };
        infoBorder.add(deployedByLabel);

        //Add markup container in case we need to set the remote repo url
        WebMarkupContainer urlLabelContainer = new WebMarkupContainer("urlLabel");
        WebMarkupContainer urlContainer = new WebMarkupContainer("url");
        infoBorder.add(urlLabelContainer);
        infoBorder.add(urlContainer);

        if (isCache) {
            urlLabelContainer.replaceWith(new Label("urlLabel", "Remote URL: "));
            String remoteRepoUrl = remoteRepo.getUrl();
            if ((remoteRepoUrl != null) && (!StringUtils.endsWith(remoteRepoUrl, "/"))) {
                remoteRepoUrl += "/";
                if (repoItem instanceof FolderActionableItem) {
                    remoteRepoUrl += ((FolderActionableItem) repoItem).getCanonicalPath().getPath();
                } else {
                    remoteRepoUrl += repoItem.getRepoPath().getPath();
                }
            }
            ExternalLink externalLink = new ExternalLink("url", remoteRepoUrl, remoteRepoUrl);
            urlContainer.replaceWith(externalLink);
        }

        final boolean isOffline = remoteRepo == null || remoteRepo.isOffline();
        final boolean globalOffline = centralConfigService.getDescriptor().isOfflineMode();
        final boolean isCacheRepo = itemIsRepo && isCache;
        String status = (isOffline || globalOffline) ? "Offline" : "Online";
        LabeledValue offlineLabel = new LabeledValue("status", "Online Status: ", status) {
            @Override
            public boolean isVisible() {
                return isCacheRepo;
            }
        };
        infoBorder.add(offlineLabel);

        final boolean repoIsBlackedOut = repoDescriptor.isBlackedOut();
        LabeledValue blackListedLabel = new LabeledValue("blackListed", "This repository is black-listed!") {
            @Override
            public boolean isVisible() {
                return itemIsRepo && repoIsBlackedOut;
            }
        };
        infoBorder.add(blackListedLabel);

        long artifactCount = 0;
        if (itemIsRepo) {
            ArtifactCount count = repositoryService.getArtifactCount(itemDisplayName);
            artifactCount = count.getCount();
        }

        LabeledValue artifactCountLabel = new LabeledValue("artifactCount", "Artifact Count: ",
                Long.toString(artifactCount)) {
            @Override
            public boolean isVisible() {
                return itemIsRepo;
            }
        };
        infoBorder.add(artifactCountLabel);

        WatchAddon watchAddon = addonsManager.addonByType(WatchAddon.class);
        RepoPath selectedPath;

        if ((itemInfo.isFolder()) && (repoItem instanceof FolderActionableItem)) {
            selectedPath = ((FolderActionableItem) repoItem).getCanonicalPath();
        } else {
            selectedPath = itemInfo.getRepoPath();
        }

        infoBorder.add(watchAddon.getWatchingSinceLabel("watchingSince", selectedPath));
        infoBorder.add(watchAddon.getDirectlyWatchedPathPanel("watchedPath", selectedPath));
        RepoPath path;
        if (repoItem instanceof FolderActionableItem) {
            path = ((FolderActionableItem) repoItem).getCanonicalPath();
        } else {
            path = repoItem.getRepoPath();
        }
        LabeledValue repoPath = new LabeledValue("repoPath", "Repository Path: ", path + "");
        infoBorder.add(repoPath);
        // disable/enable and set info according to the node type
        if (itemInfo.isFolder()) {
            ageLabel.setVisible(false);
            sizeLabel.setVisible(false);
            groupIdLabel.setVisible(false);
            artifactIdLabel.setVisible(false);
            versionLabel.setVisible(false);
        } else {
            FileInfo file = (FileInfo) itemInfo;
            MavenArtifactInfo mavenInfo =
                    ContextHelper.get().getRepositoryService().getMavenArtifactInfo(itemInfo);
            long size = file.getSize();
            //If we are looking at a cached item, check the expiry from the remote repository
            String ageStr = DurationFormatUtils.formatDuration(file.getAge(), "d'd' H'h' m'm' s's'");
            ageLabel.setValue(ageStr);
            sizeLabel.setValue(FileUtils.byteCountToDisplaySize(size));
            groupIdLabel.setValue(mavenInfo.getGroupId());
            artifactIdLabel.setValue(mavenInfo.getArtifactId());
            versionLabel.setValue(mavenInfo.getVersion());
        }
    }
}
