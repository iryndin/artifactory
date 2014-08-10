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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.feedback.UnescapedFeedbackMessage;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;

import java.util.List;

/**
 * Displays a list of repositories that are available to the user as a copy target
 *
 * @author Noam Y. Tenne
 */
public class CopyPathPanel extends MoveAndCopyBasePanel {

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repoService;

    /**
     * @param id         Panel ID
     * @param pathToCopy Path to copy
     */
    public CopyPathPanel(String id, RepoPath pathToCopy) {
        super(id, pathToCopy);
        init();
    }

    @Override
    protected TitledAjaxSubmitLink createSubmitButton(Form form, String wicketId) {
        return new TitledAjaxSubmitLink(wicketId, "Copy", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String targetRepoKey = getSelectedTargetRepository();

                MoveMultiStatusHolder status = repoService.copy(sourceRepoPath, targetRepoKey, false);

                if (!status.isError() && !status.hasWarnings()) {
                    getPage().info("Successfully copied '" + sourceRepoPath + "'." + " to '" + targetRepoKey + "'");
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
                                warnings.size() + " warnings have been produced during the copy. Please " +
                                        "review the " + logs + " for further information."));
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        getPage().error("Failed to copy '" + sourceRepoPath + "': " + message);
                    }
                }

                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }
        };
    }

    @Override
    protected List<LocalRepoDescriptor> getDeployableLocalRepoKeys() {
        return getDeployableLocalRepoKeysExcludingSource(sourceRepoPath.getRepoKey());
    }

    @Override
    protected MoveMultiStatusHolder executeDryRun(String targetRepoKey) {
        return repoService.copy(sourceRepoPath, targetRepoKey, true);
    }
}
