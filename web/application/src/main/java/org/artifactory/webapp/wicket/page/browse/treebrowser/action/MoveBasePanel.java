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

package org.artifactory.webapp.wicket.page.browse.treebrowser.action;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;

import java.util.List;

/**
 * This panel displays a list of local repositories the user can select to move a path or paths to.
 *
 * @author Yossi Shaul
 */
public abstract class MoveBasePanel extends Panel {

    private DropDownChoice targetRepos;

    public MoveBasePanel(String id) {
        super(id);
    }

    protected void init() {
        Form form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        List<LocalRepoDescriptor> localRepos = getDeployableLocalReposKeys();
        targetRepos = new DropDownChoice("targetRepos", new Model(), localRepos);
        targetRepos.setLabel(new Model("Target Repository"));
        targetRepos.setRequired(true);
        form.add(targetRepos);

        Label dryRunResult = new Label("dryRunResult", "");
        dryRunResult.setVisible(false);
        dryRunResult.setEscapeModelStrings(false);
        form.add(dryRunResult);

        form.add(new ModalCloseLink("cancel"));
        form.add(createSubmitButton(form, "move"));
        form.add(createDryRunButton(form, "dryRun"));
    }

    protected String getSelectedTargetRepository() {
        return targetRepos.getModelObjectAsString();
    }

    protected abstract TitledAjaxSubmitLink createSubmitButton(Form form, String wicketId);

    protected abstract List<LocalRepoDescriptor> getDeployableLocalReposKeys();

    protected abstract MoveMultiStatusHolder executeDryRun(String targetRepoKey);

    protected TitledAjaxSubmitLink createDryRunButton(Form form, String wicketId) {
        return new TitledAjaxSubmitLink(wicketId, "Dry Run", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String targetRepoKey = getSelectedTargetRepository();

                MoveMultiStatusHolder status = executeDryRun(targetRepoKey);

                StringBuilder result = new StringBuilder();
                if (!status.isError() && !status.hasWarnings()) {
                    result.append(("<div class='info'>Dry run completed successfully with no errors.</div>"));
                } else {
                    result.append(("<div class='title'>Dry run completed with the following errors/warnings:</div>"));
                    if (status.isError()) {
                        List<StatusEntry> errors = status.getErrors();
                        for (StatusEntry error : errors) {
                            result.append("<div class='notice'>Error: ")
                                    .append(Strings.escapeMarkup(error.getMessage(), false, false))
                                    .append("</div>");
                        }
                    }
                    if (status.hasWarnings()) {
                        List<StatusEntry> warnings = status.getWarnings();
                        for (StatusEntry warning : warnings) {
                            result.append("<div class='notice'>Warning: ")
                                    .append(Strings.escapeMarkup(warning.getMessage(), false, false))
                                    .append("</div>");
                        }
                    }
                }

                Component dryRunResult = form.get("dryRunResult");
                dryRunResult.setModelObject(result.toString());
                dryRunResult.setVisible(true);

                target.addComponent(form);
                AjaxUtils.refreshFeedback(target);
                ModalHandler.resizeAndCenterCurrent(target);
            }
        };
    }
}