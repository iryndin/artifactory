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
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.utils.ExceptionUtils;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freddy33
 */
public class BasicImportPanel extends TitledPanel {
    private static final Logger log = LoggerFactory.getLogger(BasicImportPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @WicketProperty
    private String targetRepoKey;

    @WicketProperty
    private File importFromPath;

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

    private Form createImportForm() {
        Form importForm = new Form("importForm");
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final IModel targetRepoModel = new PropertyModel(this, "targetRepoKey");
        final List<LocalRepoDescriptor> localRepos =
                repositoryService.getLocalAndCachedRepoDescriptors();
        final List<String> repoKeys = new ArrayList<String>(localRepos.size() + 1);
        //Add the "All" pseudo repository
        repoKeys.add(ImportExportReposPage.ALL_REPOS);
        for (LocalRepoDescriptor localRepo : localRepos) {
            String key = localRepo.getKey();
            repoKeys.add(key);
        }
        DropDownChoice targetRepoDdc =
                new DropDownChoice("targetRepo", targetRepoModel, repoKeys) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return ImportExportReposPage.ALL_REPOS;
                    }
                };
        //Needed because get getDefaultChoice does not update the actual selection object
        targetRepoDdc.setModelObject(ImportExportReposPage.ALL_REPOS);
        importForm.add(targetRepoDdc);
        SimpleButton importButton = new SimpleButton("import", importForm, "Import") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    //If we chose "All" import all local repositories, else import a single repo
                    File folder = importFromPath;
                    final StatusHolder status = new StatusHolder();
                    ImportSettings importSettings = new ImportSettings(folder);
                    // TODO: Add check boxes for these falgs
                    importSettings.setFailFast(true);
                    importSettings.setFailIfEmpty(true);
                    if (ImportExportReposPage.ALL_REPOS.equals(targetRepoKey)) {
                        repositoryService.importAll(importSettings, status);
                    } else {
                        repositoryService.importRepo(targetRepoKey, importSettings, status);
                    }
                    if (status.isError()) {
                        String msg = errorImportFeedback(status.getStatusMsg());
                        if (status.getException() != null) {
                            log.warn(msg, status.getException());
                        }
                    } else {
                        info("Successfully imported '" + importFromPath + "' into '" +
                                targetRepoKey +
                                "'.");
                    }
                } catch (Exception e) {
                    Throwable cause = ExceptionUtils.unwrapThrowablesOfTypes(
                            e, RepositoryRuntimeException.class, RuntimeException.class);
                    String exceptionMessage = cause.getMessage();
                    String msg = errorImportFeedback(exceptionMessage);
                    log.warn(msg, e);
                }
                FeedbackUtils.refreshFeedback(target);
                target.addComponent(form);
            }

            private String errorImportFeedback(String exceptionMessage) {
                String msg = "Failed to import '" + importFromPath + "' into '" +
                        targetRepoKey + "'. Cause: " + exceptionMessage;
                error(msg);
                return msg;
            }
        };
        importForm.add(importButton);
        importForm.add(new DefaultButtonBehavior(importButton));
        return importForm;
    }
}
