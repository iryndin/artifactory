/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.importexport.repos;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.repo.BackupService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.file.path.PathMask;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ExportRepoPanel extends TitledPanel {
    private static final Logger log = LoggerFactory.getLogger(ExportRepoPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private BackupService backupService;

    @WicketProperty
    private String sourceRepoKey;

    @WicketProperty
    private File exportToPath;

    @WicketProperty
    private boolean m2Compatible;

    public ExportRepoPanel(String string) {
        super(string);
        Form exportForm = new Form("exportForm");
        add(exportForm);

        IModel sourceRepoModel = new PropertyModel(this, "sourceRepoKey");
        List<LocalRepoDescriptor> localRepos = repositoryService.getLocalAndCachedRepoDescriptors();
        final List<String> repoKeys = new ArrayList<String>(localRepos.size() + 1);
        //Add the "All" pseudo repository
        repoKeys.add(ImportExportReposPage.ALL_REPOS);
        for (LocalRepoDescriptor localRepo : localRepos) {
            String key = localRepo.getKey();
            repoKeys.add(key);
        }
        DropDownChoice sourceRepoDdc =
                new DropDownChoice("sourceRepo", sourceRepoModel, repoKeys) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return ImportExportReposPage.ALL_REPOS;
                    }
                };
        //Needed because get getDefaultChoice does not update the actual selection object
        sourceRepoDdc.setModelObject(ImportExportReposPage.ALL_REPOS);
        exportForm.add(sourceRepoDdc);

        PropertyModel pathModel = new PropertyModel(this, "exportToPath");
        final PathAutoCompleteTextField exportToPathTf =
                new PathAutoCompleteTextField("exportToPath", pathModel);
        exportToPathTf.setMask(PathMask.FOLDERS);
        exportToPathTf.setRequired(true);
        exportForm.add(exportToPathTf);

        FileBrowserButton browserButton = new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(exportToPathTf);
            }
        };
        browserButton.setMask(PathMask.FOLDERS);
        exportForm.add(browserButton);

        exportForm.add(new StyledCheckbox("m2Compatible", new PropertyModel(this, "m2Compatible")));

        SimpleButton exportButton = new SimpleButton("export", exportForm, "Export") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    //If we chose "All" run manual backup to dest dir, else export a single repo
                    ExportSettings exportSettings = new ExportSettings(exportToPath);
                    exportSettings.setM2Compatible(m2Compatible);
                    MultiStatusHolder status = new MultiStatusHolder();
                    if (ImportExportReposPage.ALL_REPOS.equals(sourceRepoKey)) {
                        backupService.backupRepos(exportToPath, exportSettings, status);
                    } else {
                        repositoryService.exportRepo(sourceRepoKey, exportSettings, status);
                    }
                    List<StatusEntry> warnings = status.getWarnings();
                    if (!warnings.isEmpty()) {
                        warn(warnings.size() + " Warnings have been produces during the export. " +
                                "Please review the log for further information.");
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        error("Failed to export from: " + sourceRepoKey + "' to '" + exportToPath + "'. Cause: " +
                                message);
                    } else {
                        info("Successfully exported '" + sourceRepoKey + "' to '" + exportToPath + "'.");
                    }
                } catch (Exception e) {
                    String message = "Exception occured during export: ";
                    error(message + e.getMessage());
                    log.error(message, e);
                }
                FeedbackUtils.refreshFeedback(target);
                target.addComponent(form);
            }
        };
        exportForm.add(exportButton);
        exportForm.add(new DefaultButtonBehavior(exportButton));
    }
}
