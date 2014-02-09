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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SavedSearchResults;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.repo.LocalRepoAlphaComparator;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This panel displays a list of local repositories the user can select to move a path or paths to.
 *
 * @author Yossi Shaul
 */
public abstract class MoveAndCopyBasePanel extends Panel {

    @SpringBean
    private RepositoryService repoService;

    private DropDownChoice<LocalRepoDescriptor> targetRepos;

    private LocalRepoDescriptor selectedTreeItemRepo;

    protected RepoPath sourceRepoPath;

    public MoveAndCopyBasePanel(String id) {
        this(id, null);
    }

    public MoveAndCopyBasePanel(String id, RepoPath sourceRepoPath) {
        super(id);
        if (sourceRepoPath != null) {
            this.sourceRepoPath = sourceRepoPath;
            selectedTreeItemRepo = repoService.localRepoDescriptorByKey(sourceRepoPath.getRepoKey());
        }
    }

    protected void init() {
        Form form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        List<LocalRepoDescriptor> localRepos = getDeployableLocalRepoKeys();
        Collections.sort(localRepos, new LocalRepoAlphaComparator());
        ChoiceRenderer<LocalRepoDescriptor> choiceRenderer = new ChoiceRenderer<LocalRepoDescriptor>() {
            @Override
            public Object getDisplayValue(LocalRepoDescriptor selectedTarget) {
                StringBuilder displayValueBuilder = new StringBuilder();
                if (!areLayoutsCompatible(selectedTarget)) {
                    displayValueBuilder.append("! ");
                }
                displayValueBuilder.append(selectedTarget.getKey());
                return displayValueBuilder.toString();
            }
        };
        targetRepos = new DropDownChoice<>("targetRepos", new Model<LocalRepoDescriptor>(),
                localRepos, choiceRenderer);
        targetRepos.setLabel(Model.of("Target Repository"));
        targetRepos.setRequired(true);
        form.add(targetRepos);

        StringBuilder targetRepoHelp = new StringBuilder("Selects the target repository for the transferred items.");
        if (sourceRepoPath != null) {
            targetRepoHelp.append("\nRepositories starting with an exclamation mark ('!') indicate that not all ").
                    append("tokens\ncan be mapped between the layouts of the source repository and the marked ").
                    append("repository.\nPath translations may not work as expected.");
        }
        HelpBubble targetRepoHelpBubble = new HelpBubble("targetRepos.help", targetRepoHelp.toString());
        form.add(targetRepoHelpBubble);

        Label dryRunResult = new Label("dryRunResult", "");
        dryRunResult.setVisible(false);
        dryRunResult.setEscapeModelStrings(false);
        form.add(dryRunResult);

        form.add(new ModalCloseLink("cancel"));

        TitledAjaxSubmitLink actionButton = createSubmitButton(form, "action");
        form.add(actionButton);

        TitledAjaxSubmitLink dryRunButton = createDryRunButton(form, "dryRun");
        form.add(dryRunButton);
    }

    private boolean areLayoutsCompatible(LocalRepoDescriptor selectedTarget) {
        RepoLayout sourceLayout = (selectedTreeItemRepo != null) ? selectedTreeItemRepo.getRepoLayout() : null;
        RepoLayout targetRepo = (selectedTarget != null) ? selectedTarget.getRepoLayout() : null;

        return ((sourceLayout == null) || (targetRepo == null) ||
                RepoLayoutUtils.layoutsAreCompatible(sourceLayout, targetRepo));
    }

    protected String getSelectedTargetRepository() {
        return targetRepos.getDefaultModelObjectAsString();
    }

    protected abstract TitledAjaxSubmitLink createSubmitButton(Form form, String wicketId);

    protected abstract List<LocalRepoDescriptor> getDeployableLocalRepoKeys();

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
                dryRunResult.setDefaultModelObject(result.toString());
                dryRunResult.setVisible(true);
                target.add(MoveAndCopyBasePanel.this);
                target.add(form);
                AjaxUtils.refreshFeedback(target);
                ModalHandler.bindHeightTo("dryRunResult");
            }
        };
    }

    /**
     * Returns a key list of the deployable local repositories, excluding the given source
     *
     * @param sourceRepoKey Move/copy source repository key to exclude
     * @return List of the deployable local repositories, excluding the given source
     */
    protected List<LocalRepoDescriptor> getDeployableLocalRepoKeysExcludingSource(String sourceRepoKey) {
        // only display repositories the user has deploy permission on
        List<LocalRepoDescriptor> localRepos = repoService.getDeployableRepoDescriptors();
        // remove source repository from the targets list
        Iterator<LocalRepoDescriptor> iter = localRepos.iterator();
        while (iter.hasNext()) {
            if (iter.next().getKey().equals(sourceRepoKey)) {
                iter.remove();
            }
        }
        return localRepos;
    }

    /**
     * Returns the contained repo paths of the given search result
     *
     * @param resultName Name of search result to query
     * @return List of repo paths associated with the given search result
     */
    protected Set<RepoPath> getResultPaths(String resultName) {
        SavedSearchResults searchResults = ArtifactoryWebSession.get().getResults(resultName);

        // collect all the repo paths that needs to be handled
        Set<RepoPath> pathsToReturn = new HashSet<>();
        for (FileInfo fileInfo : searchResults.getResults()) {
            pathsToReturn.add(fileInfo.getRepoPath());
        }
        return pathsToReturn;
    }
}