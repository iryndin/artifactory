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

import org.apache.commons.collections15.OrderedMap;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.artifactory.api.common.PackagingType;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.utils.MimeTypes;
import org.artifactory.webapp.actionable.ItemAction;
import org.artifactory.webapp.actionable.ItemActionEvent;
import static org.artifactory.webapp.actionable.model.ActionDescriptor.*;
import org.artifactory.webapp.wicket.component.TextContentPanel;
import org.artifactory.webapp.wicket.utils.WebUtils;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class FileActionableItem extends RepoAwareActionableItemBase {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ActionableItemBase.class);

    private FileInfo file;

    public FileActionableItem(final FileInfo file) {
        super(file);
        this.file = file;
        OrderedMap<ActionDescriptor, ItemAction> actions = getActions();
        actions.put(DOWNLOAD, new ItemAction(DOWNLOAD.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
                FileInfo fileInfo = FileActionableItem.this.getFile();
                RepoPath repoPath = fileInfo.getRepoPath();
                String downloadUrl = WebUtils.getWicketServletContextUrl()
                        + "/" + repoPath.getRepoKey() + "/" + repoPath.getPath();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Download URL is " + downloadUrl);
                }
                AjaxRequestTarget target = e.getTarget();
                target.prependJavascript("window.location='" + downloadUrl + "';");
            }
        });
        actions.put(VIEW, new ItemAction(VIEW.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
                String content = getRepoService().getContent(getFile());
                ModalWindow textContentViewer = (ModalWindow) e.getTargetComponent();
                TextContentPanel contentPanel =
                        new TextContentPanel(textContentViewer.getContentId());
                contentPanel.setContent(content);
                textContentViewer.setContent(contentPanel);
                AjaxRequestTarget target = e.getTarget();
                textContentViewer.show(target);
            }
        });
        actions.put(REMOVE, new ItemAction(REMOVE.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
                undeploy();
            }
        });
        actions.put(ZAP, new ItemAction(ZAP.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
                zap();
            }
        });
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

    public String getIconRes() {
        String path = getRepoPath().getPath();
        if (MimeTypes.isJarVariant(path)) {
            return "/images/jar.png";
        } else if (path.endsWith(".pom")) {
            return "/images/pom.png";
        } else {
            return "/images/doc.png";
        }
    }

    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getFile().getRepoPath();
        OrderedMap<ActionDescriptor, ItemAction> actions = getActions();
        boolean canDeploy = authService.canDeploy(repoPath);
        boolean canDelete = authService.canDelete(repoPath);
        if (!canDelete) {
            actions.get(REMOVE).setEnabled(false);
        }
        if (!canDeploy) {
            actions.get(ZAP).setEnabled(false);
        } else if (!getRepo().isCache()) {
            actions.get(ZAP).setEnabled(false);
        }
        if (!getFile().getName().endsWith(".pom")) {
            //HACK
            actions.get(VIEW).setEnabled(false);
        }
    }

    public boolean hasStatsInfo() {
        //Hack
        String name = getDisplayName();
        if (PackagingType.isChecksum(name) || PackagingType.isMavenMetadata(name)) {
            return false;
        }
        return true;
    }
}