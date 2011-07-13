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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.FilteredResourcesWebAddon;
import org.artifactory.addon.wicket.LicensesWebAddon;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.common.wicket.ajax.AjaxLazyLoadSpanPanel;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.fs.ItemInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.CannonicalEnabledActionableFolder;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.actionable.model.LocalRepoActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Displays general item information. Placed inside the general info panel.
 *
 * @author Yossi Shaul
 */
public class GeneralInfoPanel extends Panel {
    private static final Logger log = LoggerFactory.getLogger(GeneralInfoPanel.class);

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private AuthorizationService authorizationService;

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

        LabeledValue nameLabel = new LabeledValue("name", "Name: ");
        infoBorder.add(nameLabel);

        String itemDisplayName = repoItem.getDisplayName();

        String pathUrl = BrowseRepoPage.getRepoPathUrl(repoItem);
        if (StringUtils.isBlank(pathUrl)) {
            pathUrl = "";
        }
        ExternalLink treeUrl = new ExternalLink("nameLink", pathUrl, itemDisplayName);
        infoBorder.add(treeUrl);
        infoBorder.add(new HelpBubble("nameLink.help",
                "Copy this link to navigate directly to the artifact in tree view."));

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

        LabeledValue deployedByLabel = new LabeledValue("deployed-by", "Deployed by: ", itemInfo.getModifiedBy()) {
            @Override
            public boolean isVisible() {
                return !itemIsRepo;
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
                return repoIsBlackedOut;
            }
        };
        infoBorder.add(blackListedLabel);

        addArtifactCount(itemIsRepo, infoBorder, itemDisplayName);

        addWatcherInfo(repoItem, infoBorder);

        final RepoPath path;
        if (repoItem instanceof FolderActionableItem) {
            path = ((FolderActionableItem) repoItem).getCanonicalPath();
        } else {
            path = repoItem.getRepoPath();
        }
        LabeledValue repoPath = new LabeledValue("repoPath", "Repository Path: ", path + "");
        infoBorder.add(repoPath);

        addItemInfoLabels(infoBorder, itemInfo);

        addLicenseInfo(infoBorder, path);

        addLocalLayoutInfo(infoBorder, repoDescriptor, itemIsRepo);
        addRemoteLayoutInfo(infoBorder, remoteRepo, itemIsRepo);

        addFilteredResourceCheckbox(infoBorder, itemInfo);
    }

    private void addArtifactCount(final boolean itemIsRepo, final FieldSetBorder infoBorder,
            final String itemDisplayName) {
        if (!itemIsRepo) {
            infoBorder.add(new WebMarkupContainer("artifactCountLabel"));
            infoBorder.add(new WebMarkupContainer("artifactCountValue"));
            WebMarkupContainer linkContainer = new WebMarkupContainer("link");
            linkContainer.setVisible(false);
            infoBorder.add(linkContainer);
        } else {
            infoBorder.add(new Label("artifactCountLabel", "Artifact Count: "));
            final WebMarkupContainer container = new WebMarkupContainer("artifactCountValue");
            infoBorder.add(container);
            AjaxLink<String> link = new AjaxLink<String>("link") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    setVisible(false);
                    container.replaceWith((new ArtifactCountLazySpanPanel("artifactCountValue", itemDisplayName)));
                    target.addComponent(infoBorder);
                }
            };
            infoBorder.add(link);
        }
    }

    private static class ArtifactCountLazySpanPanel extends AjaxLazyLoadSpanPanel {
        @SpringBean
        private RepositoryService repositoryService;

        private String itemDisplayName;

        public ArtifactCountLazySpanPanel(final String id, String itemDisplayName) {
            super(id);
            this.itemDisplayName = itemDisplayName;
            InjectorHolder.getInjector().inject(this);
        }

        @Override
        public Component getLazyLoadComponent(String markupId) {
            ArtifactCount count = repositoryService.getArtifactCount(itemDisplayName);
            return new Label(markupId, Long.toString(count.getCount()));
        }
    }

    private void addWatcherInfo(RepoAwareActionableItem repoItem, FieldSetBorder infoBorder) {
        org.artifactory.fs.ItemInfo itemInfo = repoItem.getItemInfo();
        WatchAddon watchAddon = addonsManager.addonByType(WatchAddon.class);
        RepoPath selectedPath;

        if ((itemInfo.isFolder()) && (repoItem instanceof FolderActionableItem)) {
            selectedPath = ((FolderActionableItem) repoItem).getCanonicalPath();
        } else {
            selectedPath = itemInfo.getRepoPath();
        }

        infoBorder.add(watchAddon.getWatchingSinceLabel("watchingSince", selectedPath));
        infoBorder.add(watchAddon.getDirectlyWatchedPathPanel("watchedPath", selectedPath));
    }

    private void addLicenseInfo(FieldSetBorder infoBorder, RepoPath path) {
        LicensesWebAddon licensesWebAddon = addonsManager.addonByType(LicensesWebAddon.class);
        final LabeledValue licensesLabel = licensesWebAddon.getLicenseLabel("licenses", path);
        licensesLabel.setOutputMarkupId(true);
        infoBorder.add(licensesLabel);
        AbstractLink addButton = licensesWebAddon.getAddLicenseLink("add", path,
                licensesLabel.getDefaultModelObjectAsString(), licensesLabel);
        infoBorder.add(addButton);
        AbstractLink editLicenseLink = licensesWebAddon.getEditLicenseLink("edit", path,
                licensesLabel.getDefaultModelObjectAsString(), licensesLabel);
        infoBorder.add(editLicenseLink);
        AbstractLink deleteLicenseLink = licensesWebAddon.getDeleteLink("delete", path,
                licensesLabel.getDefaultModelObjectAsString(), infoBorder);
        infoBorder.add(deleteLicenseLink);
    }

    private void addItemInfoLabels(FieldSetBorder infoBorder, ItemInfo itemInfo) {
        LabeledValue sizeLabel = new LabeledValue("size", "Size: ");
        infoBorder.add(sizeLabel);

        LabeledValue ageLabel = new LabeledValue("age", "Age: ");
        infoBorder.add(ageLabel);

        LabeledValue moduleId = new LabeledValue("moduleId", "Module ID: ");
        infoBorder.add(moduleId);

        // disable/enable and set info according to the node type
        if (itemInfo.isFolder()) {
            ageLabel.setVisible(false);
            sizeLabel.setVisible(false);
            moduleId.setVisible(false);
        } else {
            org.artifactory.fs.FileInfo file = (org.artifactory.fs.FileInfo) itemInfo;

            ModuleInfo moduleInfo = repositoryService.getItemModuleInfo(file.getRepoPath());

            long size = file.getSize();
            //If we are looking at a cached item, check the expiry from the remote repository
            String ageStr = DurationFormatUtils.formatDuration(file.getAge(), "d'd' H'h' m'm' s's'");
            ageLabel.setValue(ageStr);
            sizeLabel.setValue(StorageUnit.toReadableString(size));
            if (moduleInfo.isValid()) {
                moduleId.setValue(moduleInfo.getPrettyModuleId());
            } else {
                moduleId.setValue("N/A");
            }
        }
    }

    private void addLocalLayoutInfo(FieldSetBorder infoBorder, LocalRepoDescriptor repoDescriptor, boolean itemIsRepo) {
        LabeledValue localLayoutLabel = new LabeledValue("localLayout", "Repository Layout: ",
                repoDescriptor.getRepoLayout().getName());
        localLayoutLabel.setVisible(itemIsRepo);
        infoBorder.add(localLayoutLabel);
    }

    private void addRemoteLayoutInfo(FieldSetBorder infoBorder, RemoteRepoDescriptor remoteRepo, boolean itemIsRepo) {
        String componentId = "remoteLayout";
        if (!itemIsRepo || remoteRepo == null || remoteRepo.getRemoteRepoLayout() == null) {
            infoBorder.add(new WebMarkupContainer(componentId));
        } else {
            infoBorder.add(new LabeledValue(componentId, "Remote Repository Layout: ",
                    remoteRepo.getRemoteRepoLayout().getName()));
        }
    }

    private String getRepoPathUrl(RepoAwareActionableItem repoItem) {
        String artifactPath;
        if (repoItem instanceof CannonicalEnabledActionableFolder) {
            artifactPath = ((CannonicalEnabledActionableFolder) repoItem).getCanonicalPath().getPath();
        } else {
            artifactPath = repoItem.getRepoPath().getPath();
        }

        StringBuilder urlBuilder = new StringBuilder();
        if (NamingUtils.isChecksum(artifactPath)) {
            // if a checksum file is deployed, link to the target file
            artifactPath = MavenNaming.getChecksumTargetFile(artifactPath);
        }
        String repoPathId = new RepoPathImpl(repoItem.getRepo().getKey(), artifactPath).getId();

        String encodedPathId;
        try {
            encodedPathId = URLEncoder.encode(repoPathId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode deployed artifact ID '{}': {}.", repoPathId, e.getMessage());
            return null;
        }

        //Using request parameters instead of wicket's page parameters. See RTFACT-2843
        urlBuilder.append(WicketUtils.absoluteMountPathForPage(BrowseRepoPage.class)).append("?").
                append(BrowseRepoPage.PATH_ID_PARAM).append("=").append(encodedPathId);
        return urlBuilder.toString();
    }

    private void addFilteredResourceCheckbox(FieldSetBorder infoBorder, ItemInfo itemInfo) {
        WebMarkupContainer filteredResourceContainer = new WebMarkupContainer("filteredResourceContainer");
        filteredResourceContainer.setVisible(false);
        infoBorder.add(filteredResourceContainer);

        WebMarkupContainer filteredResourceCheckbox = new WebMarkupContainer("filteredResourceCheckbox");
        filteredResourceContainer.add(filteredResourceCheckbox);

        final WebMarkupContainer filteredResourceHelpBubble = new WebMarkupContainer("filteredResource.help");
        filteredResourceContainer.add(filteredResourceHelpBubble);

        if (!itemInfo.isFolder() && authorizationService.canAnnotate(itemInfo.getRepoPath())) {
            FilteredResourcesWebAddon filteredResourcesWebAddon =
                    addonsManager.addonByType(FilteredResourcesWebAddon.class);
            filteredResourceCheckbox.replaceWith(
                    filteredResourcesWebAddon.getFilteredResourceCheckbox("filteredResourceCheckbox", itemInfo));
            filteredResourceHelpBubble
                    .replaceWith(new HelpBubble("filteredResource.help", getString("filteredResource.help")));
            filteredResourceContainer.setVisible(true);
        }
    }
}