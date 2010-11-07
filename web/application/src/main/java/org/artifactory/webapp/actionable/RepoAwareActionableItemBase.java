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

package org.artifactory.webapp.actionable;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.BuildAddon;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.GeneralTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven.MetadataTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.permissions.PermissionsTabPanel;
import org.artifactory.webapp.wicket.panel.tabbed.TabbedPanel;
import org.artifactory.webapp.wicket.panel.tabbed.tab.BaseTab;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author Yoav Landman
 */
public abstract class RepoAwareActionableItemBase extends ActionableItemBase
        implements RepoAwareActionableItem, TabViewedActionableItem {

    private static final Logger log = LoggerFactory.getLogger(RepoAwareActionableItemBase.class);

    private final RepoPath repoPath;

    protected RepoAwareActionableItemBase(ItemInfo itemInfo) {
        this.repoPath = new RepoPathImpl(itemInfo.getRepoKey(), itemInfo.getRelPath());
    }

    protected RepoAwareActionableItemBase(RepoPath repoPath) {
        this.repoPath = repoPath;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public LocalRepoDescriptor getRepo() {
        String repoKey = repoPath.getRepoKey();
        return getRepoService().localOrCachedRepoDescriptorByKey(repoKey);
    }

    protected RepositoryService getRepoService() {
        return ContextHelper.get().beanForType(RepositoryService.class);
    }

    protected AddonsManager getAddonsProvider() {
        return ContextHelper.get().beanForType(AddonsManager.class);
    }

    public StatsInfo getStatsInfo() {
        return getXmlMetadata(StatsInfo.class);
    }

    public org.artifactory.fs.ItemInfo getItemInfo() {
        return getItemInfo(repoPath);
    }

    public org.artifactory.fs.ItemInfo getItemInfo(RepoPath repoPath) {
        return getRepoService().getItemInfo(repoPath);
    }

    public <MD> MD getXmlMetadata(Class<MD> metadataClass) {
        return getXmlMetadata(repoPath, metadataClass);
    }

    public <MD> MD getXmlMetadata(RepoPath repoPath, Class<MD> metadataClass) {
        return getRepoService().getMetadata(repoPath, metadataClass);
    }

    public Panel newItemDetailsPanel(final String id) {
        return new TabbedPanel(id) {
            @Override
            protected void addTabs(List<ITab> tabs) {
                RepoAwareActionableItemBase.this.addTabs(tabs);
            }

        };
    }

    public void addTabs(List<ITab> tabs) {
        final RepoAwareActionableItem item = this;

        AuthorizationService authService = ContextHelper.get().getAuthorizationService();
        boolean canAdminRepoPath = authService.canAdmin(repoPath);

        tabs.add(new AbstractTab(Model.of("General")) {
            @Override
            public Panel getPanel(String panelId) {
                return new GeneralTabPanel(panelId, item);
            }
        });

        //Add the permissions tab
        // only allow users with admin permissions on a target info that includes current path
        if (canAdminRepoPath) {
            tabs.add(new PermissionsTab(item));
        }

        org.artifactory.fs.ItemInfo itemInfo = item.getItemInfo();
        final RepoPath canonicalRepoPath;
        if ((itemInfo.isFolder()) && (item instanceof FolderActionableItem)) {
            canonicalRepoPath = ((FolderActionableItem) item).getCanonicalPath();
        } else {
            canonicalRepoPath = item.getRepoPath();
        }

        // add metadata view panel
        tabs.add(new AbstractTab(Model.of("Metadata")) {
            @Override
            public Panel getPanel(String panelId) {
                return new MetadataTabPanel(panelId, item, canonicalRepoPath);
            }
        });

        if (canAdminRepoPath) {
            //Add watchers tab
            WatchAddon watchAddon = getAddonsProvider().addonByType(WatchAddon.class);
            ITab watchersTab = watchAddon.getWatchersTab("Watchers", canonicalRepoPath);
            tabs.add(watchersTab);
        }


        if (!itemInfo.isFolder() && itemInfo instanceof FileInfo) {
            BuildAddon buildAddon = getAddonsProvider().addonByType(BuildAddon.class);
            tabs.add(buildAddon.getBuildsTab(item));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepoAwareActionableItemBase)) {
            return false;
        }
        RepoAwareActionableItemBase base = (RepoAwareActionableItemBase) o;
        return repoPath.equals(base.repoPath);
    }

    @Override
    public int hashCode() {
        return repoPath.hashCode();
    }

    private static class PermissionsTab extends BaseTab {
        private final RepoAwareActionableItem item;

        private PermissionsTab(RepoAwareActionableItem item) {
            super(Model.of("Effective Permissions"));
            this.item = item;
        }

        @Override
        public Panel getPanel(String panelId) {
            return new PermissionsTabPanel(panelId, item);
        }

        @Override
        public void onNewTabLink(Component link) {
            super.onNewTabLink(link);
            link.add(new CssClass("permissions-tab"));
        }
    }
}
