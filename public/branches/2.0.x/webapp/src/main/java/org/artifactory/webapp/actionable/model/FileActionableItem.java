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

package org.artifactory.webapp.actionable.model;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.DownloadAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.RemoveAction;
import org.artifactory.webapp.actionable.action.ViewAction;
import org.artifactory.webapp.actionable.action.ZapAction;
import org.artifactory.webapp.wicket.common.component.panel.actionable.StatsTabPanel;
import org.artifactory.webapp.wicket.common.component.panel.actionable.maven.PomViewTabPanel;
import org.artifactory.webapp.wicket.utils.CssClass;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class FileActionableItem extends RepoAwareActionableItemBase {

    private FileInfo file;
    private ItemAction downloadAction;
    private ItemAction viewAction;
    private ItemAction removeAction;
    private ItemAction zapAction;

    public FileActionableItem(FileInfo file) {
        super(file);
        this.file = file;
        Set<ItemAction> actions = getActions();
        downloadAction = new DownloadAction();
        actions.add(downloadAction);
        viewAction = new ViewAction();
        actions.add(viewAction);
        removeAction = new RemoveAction();
        actions.add(removeAction);
        zapAction = new ZapAction();
        actions.add(zapAction);
    }

    public FileInfo getFile() {
        if (file == null) {
            file = (FileInfo) getItemInfo();
        }
        return file;
    }

    public String getDisplayName() {
        return getFile().getName();
    }

    public String getCssClass() {
        String path = getRepoPath().getPath();
        return CssClass.getFileCssClass(path).cssClass();
    }

    @Override
    public void addTabs(List<ITab> tabs) {
        super.addTabs(tabs);
        boolean showTabs = shouldShowTabs();
        if (showTabs) {
            return;
        }
        //Has stats
        tabs.add(new AbstractTab(new Model("Stats")) {
            @Override
            public Panel getPanel(String panelId) {
                return new StatsTabPanel(panelId, FileActionableItem.this);
            }
        });

        if (isPomFile()) {
            // add pom view panel
            tabs.add(new AbstractTab(new Model("Pom View")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new PomViewTabPanel(panelId, FileActionableItem.this);
                }
            });
        }

    }

    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getFile().getRepoPath();
        boolean canAdmin = authService.canAdmin(repoPath);
        boolean canDelete = authService.canDelete(repoPath);
        if (!canDelete) {
            removeAction.setEnabled(false);
        }
        if (!canAdmin) {
            zapAction.setEnabled(false);
        } else if (!getRepo().isCache()) {
            zapAction.setEnabled(false);
        }
        if (!isPomFile()) {
            //HACK
            viewAction.setEnabled(false);
        }
        downloadAction.setEnabled(true);
    }

    private boolean isPomFile() {
        return getFile().getName().endsWith(".pom");
    }

    private boolean shouldShowTabs() {
        //Hack - dont display anything for checksums or metadata
        String name = getDisplayName();
        return NamingUtils.isChecksum(name) || NamingUtils.isMetadata(name);
    }
}
