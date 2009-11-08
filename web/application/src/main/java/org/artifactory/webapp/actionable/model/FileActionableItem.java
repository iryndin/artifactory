/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.actionable.model;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.ivy.IvyNaming;
import org.artifactory.webapp.actionable.FileActionable;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.CopyAction;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.DownloadAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.MoveAction;
import org.artifactory.webapp.actionable.action.ViewTextFileAction;
import org.artifactory.webapp.actionable.action.ZapAction;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.ivy.XmlViewTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.jnlp.JnlpViewTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven.PomViewTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.stats.StatsTabPanel;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.List;
import java.util.Set;

import static org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.ivy.XmlViewTabPanel.XmlTypes.GENERAL_XML;
import static org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.ivy.XmlViewTabPanel.XmlTypes.IVY_XML;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class FileActionableItem extends RepoAwareActionableItemBase implements FileActionable {

    private final FileInfo fileInfo;
    private ItemAction downloadAction;
    private ItemAction viewAction;

    private ItemAction deleteAction;
    private ItemAction zapAction;
    private MoveAction moveAction;
    private CopyAction copyAction;
    private ItemAction watchAction;

    public FileActionableItem(FileInfo fileInfo) {
        super(fileInfo);
        this.fileInfo = fileInfo;
        Set<ItemAction> actions = getActions();
        viewAction = new ViewTextFileAction();
        actions.add(viewAction);
        downloadAction = new DownloadAction();
        actions.add(downloadAction);
        moveAction = new MoveAction();
        actions.add(moveAction);
        copyAction = new CopyAction();
        actions.add(copyAction);
        zapAction = new ZapAction();
        actions.add(zapAction);
        deleteAction = new DeleteAction();
        actions.add(deleteAction);

        AddonsManager addonsManager = getAddonsProvider();
        WatchAddon watchAddon = addonsManager.addonByType(WatchAddon.class);
        watchAction = watchAddon.getWatchAction(fileInfo.getRepoPath());
        actions.add(watchAction);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getDisplayName() {
        return getFileInfo().getName();
    }

    public String getCssClass() {
        String path = getRepoPath().getPath();
        return ItemCssClass.getFileCssClass(path).getCssClass();
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

        if (isXmlFile() && !isPomFile()) {
            //xml tab
            final XmlViewTabPanel.XmlTypes xmlType = isIvyFile() ? IVY_XML : GENERAL_XML;
            tabs.add(new AbstractTab(new Model(xmlType.getTabTitle())) {

                @Override
                public Panel getPanel(String panelId) {
                    return new XmlViewTabPanel(panelId, FileActionableItem.this, xmlType);
                }
            });

            if (isJnlpFile()) {
                tabs.add(new AbstractTab(new Model("JNLP")) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new JnlpViewTabPanel(panelId, FileActionableItem.this);
                    }
                });
            }
        }
    }

    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getFileInfo().getRepoPath();
        boolean canAdmin = authService.canAdmin(repoPath);
        boolean canDelete = authService.canDelete(repoPath);
        boolean canRead = authService.canRead(repoPath);
        if (!canDelete) {
            deleteAction.setEnabled(false);
        }
        if (!canAdmin) {
            zapAction.setEnabled(false);
        } else if (!getRepo().isCache()) {
            zapAction.setEnabled(false);
        }
        if (!isPomFile() && !isXmlFile()) {
            viewAction.setEnabled(false);
        }

        if (isJnlpFile()) {
            downloadAction.setEnabled(false);
        }

        if (!canDelete || NamingUtils.isSystem(repoPath.getPath()) ||
                !authService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            moveAction.setEnabled(false);
        }

        if (!canDelete || NamingUtils.isSystem(repoPath.getPath()) ||
                !authService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            copyAction.setEnabled(false);
        }

        if (!canRead || authService.isAnonymous()) {
            watchAction.setEnabled(false);
        }
    }

    private boolean isXmlFile() {
        return NamingUtils.getContentType(getFileInfo().getName()).isXml();
    }

    private boolean isIvyFile() {
        return IvyNaming.isIvyFileName(getFileInfo().getName());
    }

    private boolean isPomFile() {
        return MavenNaming.isPom((getFileInfo().getName()));
    }

    private boolean isJnlpFile() {
        return NamingUtils.getContentType((getFileInfo().getName())).isJnlp();
    }


    private boolean shouldShowTabs() {
        //Hack - dont display anything for checksums or metadata
        String name = getDisplayName();
        return NamingUtils.isChecksum(name) || NamingUtils.isMetadata(name);
    }
}
