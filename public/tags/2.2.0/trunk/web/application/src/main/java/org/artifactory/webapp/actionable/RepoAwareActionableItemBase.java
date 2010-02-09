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
import org.artifactory.addon.wicket.PropertiesAddon;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.GeneralTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven.MetadataTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.permissions.PermissionsTabPanel;
import org.artifactory.webapp.wicket.panel.tabbed.TabbedPanel;
import org.artifactory.webapp.wicket.panel.tabbed.tab.BaseTab;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class RepoAwareActionableItemBase extends ActionableItemBase
        implements RepoAwareActionableItem, TabViewedActionableItem {

    private final RepoPath repoPath;

    protected RepoAwareActionableItemBase(ItemInfo itemInfo) {
        this.repoPath = new RepoPath(itemInfo.getRepoKey(), itemInfo.getRelPath());
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

    public ItemInfo getItemInfo() {
        return getItemInfo(repoPath);
    }

    public ItemInfo getItemInfo(RepoPath repoPath) {
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

        tabs.add(new AbstractTab(new Model("General")) {
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

        //Add properties panel
        PropertiesAddon propertiesAddon = getAddonsProvider().addonByType(PropertiesAddon.class);

        ItemInfo info;
        if (item instanceof FolderActionableItem) {
            // take the last element if folder compacted compacted
            info = ((FolderActionableItem) item).getFolderInfo();
        } else {
            info = item.getItemInfo();
        }

        ITab propertiesPanel = propertiesAddon.getPropertiesTabPanel(info);
        tabs.add(propertiesPanel);

        ItemInfo itemInfo = item.getItemInfo();
        final RepoPath canonicalRepoPath;
        if ((itemInfo.isFolder()) && (item instanceof FolderActionableItem)) {
            canonicalRepoPath = ((FolderActionableItem) item).getCanonicalPath();
        } else {
            canonicalRepoPath = item.getRepoPath();
        }

        final List<String> typeList = getMetadataNames(canonicalRepoPath);
        //Display the metadata tab only if there is any associated with the item
        if (!typeList.isEmpty()) {
            // add metadata view panel
            tabs.add(new AbstractTab(new Model("Metadata")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new MetadataTabPanel(panelId, canonicalRepoPath, typeList);
                }
            });
        }

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

    /**
     * Returns a list of metadata type names that are associated with this item
     *
     * @return
     */
    private List<String> getMetadataNames(RepoPath canonicalRepoPath) {
        List<String> metadataNames = getRepoService().getMetadataNames(canonicalRepoPath);
        return metadataNames;
    }

    private static class PermissionsTab extends BaseTab {
        private final RepoAwareActionableItem item;

        private PermissionsTab(RepoAwareActionableItem item) {
            super(new Model("Effective Permissions"));
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