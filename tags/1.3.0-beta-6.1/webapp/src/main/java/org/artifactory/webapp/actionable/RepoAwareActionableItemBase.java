/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable;

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.common.component.panel.actionable.GeneralTabPanel;
import org.artifactory.webapp.wicket.common.component.panel.actionable.PermissionsTabPanel;
import org.artifactory.webapp.wicket.common.component.panel.actionable.StyledTabbedPanel;
import org.artifactory.webapp.wicket.common.component.panel.actionable.TabbedPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;

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
        return getRepoService().getXmlMetdataObject(repoPath, metadataClass);
    }

    public Panel newItemDetailsPanel(final String id) {
        return new TabbedPanel(id) {
            @Override
            protected void addTabs(List<ITab> tabs) {
                RepoAwareActionableItemBase.this.addTabs(tabs);
            }

            @SuppressWarnings({"unchecked"})
            @Override
            protected StyledTabbedPanel newTabbedPanel(List<ITab> tabs) {
                StyledTabbedPanel tabbedPanel = new StyledTabbedPanel("tabs", tabs) {
                    @Override
                    protected void onAjaxUpdate(AjaxRequestTarget target) {
                        super.onAjaxUpdate(target);

                        // store last tab name on page
                        BrowseRepoPage page = (BrowseRepoPage) RequestCycle.get().getResponsePage();
                        ITab tab = (ITab) getTabs().get(getSelectedTab());
                        page.setLastTabName(tab.getTitle().getObject().toString());
                        target.appendJavascript("Browser.fixTabPanel()");
                    }

                };
                //TODO: [by yl] This logic doesn't belong here!
                //Need to move to a special subclass (something like PersistentTabbedPanel) and use
                //a shared cookie to store the state, not the page
                // reselect last tab by stored name
                String lastTabName = null;
                Page page = RequestCycle.get().getResponsePage();
                if (page instanceof BrowseRepoPage) {
                    lastTabName = ((BrowseRepoPage)page).getLastTabName();
                }

                int i = 0;
                for (ITab tab : (List<ITab>) tabbedPanel.getTabs()) {
                    String tabName = tab.getTitle().getObject().toString();
                    if (tabName.equals(lastTabName)) {
                        tabbedPanel.setSelectedTab(i);
                    }
                    i++;
                }
                return tabbedPanel;
            }
        };
    }

    public void addTabs(List<ITab> tabs) {
        final RepoAwareActionableItem item = this;
        tabs.add(new AbstractTab(new Model("General")) {
            @Override
            public Panel getPanel(String panelId) {
                return new GeneralTabPanel(panelId, item);
            }
        });
        //Add the permissions tab
        AuthorizationService authService = ContextHelper.get().getAuthorizationService();
        // only allow users with admin permissions on a target info that includes current path
        if (authService.canAdmin(repoPath)) {
            tabs.add(new AbstractTab(new Model("Effective Permissions")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new PermissionsTabPanel(panelId, item);
                }
            });
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
}