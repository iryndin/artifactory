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

package org.artifactory.webapp.wicket.page.browse.treebrowser.action;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.feedback.UnescapedFeedbackMessage;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;

import java.util.List;

/**
 * This panel displays a list of local repositories the user can select to move a path to.
 *
 * @author Yossi Shaul
 */
public class MovePathPanel extends MoveAndCopyBasePanel {

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repoService;

    private final Component componentToRefresh;
    private final TreeBrowsePanel browseRepoPanel;

    public MovePathPanel(String id, RepoPath pathToMove, Component componentToRefresh,
            TreeBrowsePanel browseRepoPanel) {
        super(id, pathToMove);
        this.componentToRefresh = componentToRefresh;
        this.browseRepoPanel = browseRepoPanel;
        init();
    }

    @Override
    protected TitledAjaxSubmitLink createSubmitButton(Form form, String wicketId) {
        return new TitledAjaxSubmitLink(wicketId, "Move", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String targetRepoKey = getSelectedTargetRepository();

                MoveMultiStatusHolder status = repoService.move(sourceRepoPath, targetRepoKey, false);

                if (!status.isError() && !status.hasWarnings()) {
                    getPage().info("Successfully moved '" + sourceRepoPath + "' to '" + targetRepoKey + "'.");
                } else {
                    if (status.hasWarnings()) {
                        List<StatusEntry> warnings = status.getWarnings();
                        String logs;
                        if (authorizationService.isAdmin()) {
                            String systemLogsPage = WicketUtils.absoluteMountPathForPage(SystemLogsPage.class);
                            logs = "<a href=\"" + systemLogsPage + "\">log</a>";
                        } else {
                            logs = "log";
                        }
                        getPage().warn(new UnescapedFeedbackMessage(
                                warnings.size() + " warnings have been produced during the move. Please " +
                                        "review the " + logs + " for further information."));
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        getPage().error("Failed to move '" + sourceRepoPath + "': " + message);
                    }
                }

                // colapse all tree nodes
                if (componentToRefresh instanceof Tree) {
                    // we collapse all since we don't know which path will eventually move
                    Tree tree = (Tree) componentToRefresh;
                    ITreeState treeState = tree.getTreeState();
                    treeState.collapseAll();
                }

                browseRepoPanel.removeNodePanel(target);
                target.add(componentToRefresh);
                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                // add confirmation dialog when clicked
                String message = String.format("Are you sure you wish to move '%s'?", sourceRepoPath);
                return new ConfirmationAjaxCallDecorator(message);
            }
        };
    }

    @Override
    protected MoveMultiStatusHolder executeDryRun(String targetRepoKey) {
        return repoService.move(sourceRepoPath, targetRepoKey, true);
    }

    @Override
    protected List<LocalRepoDescriptor> getDeployableLocalRepoKeys() {
        return getDeployableLocalRepoKeysExcludingSource(sourceRepoPath.getRepoKey());
    }
}