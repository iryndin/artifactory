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

package org.artifactory.webapp.wicket.page.importexport.repos;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freddy33
 */
public abstract class BasicImportPanel extends TitledPanel {
    private static final Logger log = LoggerFactory.getLogger(BasicImportPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private SearchService searchService;

    @WicketProperty
    private String targetRepoKey;

    @WicketProperty
    private File importFromPath;

    @WicketProperty
    private boolean verbose;

    @WicketProperty
    private boolean excludeMetadata;

    @WicketProperty
    private boolean trustServerChecksums;

    private Form importForm;

    public BasicImportPanel(String id) {
        super(id);
        importForm = createImportForm();
    }

    public Form getImportForm() {
        return importForm;
    }

    public File getImportFromPath() {
        return importFromPath;
    }

    public void setImportFromPath(File importFromPath) {
        this.importFromPath = importFromPath;
    }

    public String getTargetRepoKey() {
        return targetRepoKey;
    }

    private Form createImportForm() {
        Form importForm = new Form("importForm");
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final IModel targetRepoModel = new PropertyModel(this, "targetRepoKey");
        List<LocalRepoDescriptor> localRepos =
                repositoryService.getLocalAndCachedRepoDescriptors();
        final List<String> repoKeys = new ArrayList<String>(localRepos.size() + 1);
        //Add the "All" pseudo repository
        repoKeys.add(ImportExportReposPage.ALL_REPOS);
        for (LocalRepoDescriptor localRepo : localRepos) {
            String key = localRepo.getKey();
            repoKeys.add(key);
        }
        DropDownChoice targetRepoDdc = new DropDownChoice("targetRepo", targetRepoModel, repoKeys);
        targetRepoDdc.setDefaultModelObject(ImportExportReposPage.ALL_REPOS);
        importForm.add(targetRepoDdc);
        final MultiStatusHolder status = new MultiStatusHolder();
        TitledAjaxSubmitLink importButton = new TitledAjaxSubmitLink("import", "Import", importForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                Session.get().cleanupFeedbackMessages();
                onBeforeImport();

                boolean importAllRepos = ImportExportReposPage.ALL_REPOS.equals(targetRepoKey);
                File folder = importFromPath;
                if (importAllRepos) {
                    //If the user has by mistake selected the root backup dir: RTFACT-3480
                    File repositoriesExportDir = new File(importFromPath, "repositories");
                    if (repositoriesExportDir.isDirectory()) {
                        folder = repositoriesExportDir;
                    }
                }

                status.reset();
                status.setVerbose(verbose);

                ImportSettings importSettings = new ImportSettings(folder, status);
                try {
                    importSettings.setFailIfEmpty(true);
                    importSettings.setVerbose(verbose);
                    importSettings.setIncludeMetadata(!excludeMetadata);
                    importSettings.setTrustServerChecksums(trustServerChecksums);
                    //If we chose "All" import all local repositories, else import a single repo
                    if (importAllRepos) {
                        //Do not activate archive indexing until all repositories were imported
                        repositoryService.importAll(importSettings);
                    } else {
                        //We enable the archive indexing from within the import job since we know there will be only one
                        importSettings.setIndexMarkedArchives(true);
                        repositoryService.importRepo(targetRepoKey, importSettings);
                    }
                    List<StatusEntry> errors = status.getErrors();
                    List<StatusEntry> warnings = status.getWarnings();
                    CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                    String logs = "Please review the <a href=\"" + systemLogsPage +
                            "\">log</a> for further information.";
                    if (!errors.isEmpty()) {
                        error(errors.size() + " error(s) reported during the import. " + logs);
                    } else if (!warnings.isEmpty()) {
                        warn(warnings.size() + " warning(s) reported during the import. " + logs);
                    } else {
                        info("Successfully imported '" + importFromPath + "' into '" + targetRepoKey + "'.");
                    }
                } catch (Exception e) {
                    status.setError(e.getMessage(), log);
                    errorImportFeedback(status);
                } finally {
                    if (!importSettings.isIndexMarkedArchives()) {
                        searchService.asyncIndexMarkedArchives();
                    }
                    cleanupResources();
                }
                refreshImportPanel(form, target);
            }

            private void errorImportFeedback(MultiStatusHolder status) {
                String error = status.getStatusMsg();
                Throwable exception = status.getException();
                if (exception != null) {
                    error = exception.getMessage();
                }
                String msg = "Failed to import '" + importFromPath + "' into '" +
                        targetRepoKey + "'. Cause: " + error;
                error(msg);
            }
        };
        importForm.add(importButton);
        importForm.add(new DefaultButtonBehavior(importButton));
        return importForm;
    }

    protected void refreshImportPanel(Form form, AjaxRequestTarget target) {
        target.addComponent(form);
        AjaxUtils.refreshFeedback(target);
    }

    protected void addVerboseCheckbox(Form form) {
        StyledCheckbox verboseCheckbox = new StyledCheckbox("verbose", new PropertyModel<Boolean>(this, "verbose"));
        verboseCheckbox.setRequired(false);
        form.add(verboseCheckbox);
        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
        form.add(new HelpBubble("verboseHelp", "Lowers the log level to debug and redirects the output from the " +
                "standard log to the import-export log." +
                "\nHint: You can monitor the log in the <a href=\"" + systemLogsPage + "\">'System Logs'</a> page."));
    }

    protected abstract void onBeforeImport();

    protected abstract void cleanupResources();
}
