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
package org.artifactory.webapp.wicket.importexport.repos;

import org.apache.log4j.Logger;
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
import org.artifactory.webapp.wicket.component.SimpleAjaxSubmitLink;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freddy33
 */
public class BasicImportPanel extends TitledPanel {
    private final static Logger LOGGER = Logger.getLogger(BasicImportPanel.class);

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
        Form importForm = new Form("importForm") {
            @Override
            protected void onSubmit() {
                try {
                    //If we chose "All" import all local repositories, else import a single repo
                    File folder = importFromPath;
                    final StatusHolder status = new StatusHolder();
                    ImportSettings importSettings = new ImportSettings(folder);
                    if (ImportExportReposPage.ALL_REPOS.equals(targetRepoKey)) {
                        repositoryService.importAll(importSettings, status);
                    } else {
                        repositoryService.importRepo(targetRepoKey, importSettings, status);
                    }
                    if (status.isError()) {
                        String msg = errorImportFeedback(status.getStatusMsg());
                        if (status.getThrowable() != null) {
                            LOGGER.warn(msg, status.getThrowable());
                        }
                        return;
                    }
                } catch (Exception e) {
                    Throwable cause = ExceptionUtils.unwrapThrowablesOfTypes(
                            e, RepositoryRuntimeException.class, RuntimeException.class);
                    String exceptionMessage = cause.getMessage();
                    String msg = errorImportFeedback(exceptionMessage);
                    LOGGER.warn(msg, e);
                    return;
                }
                info("Successfully imported '" + importFromPath + "' into '" + targetRepoKey +
                        "'.");
            }

            private String errorImportFeedback(String exceptionMessage) {
                String msg = "Failed to import '" + importFromPath + "' into '" +
                        targetRepoKey + "'. Cause: " + exceptionMessage;
                error(msg);
                return msg;
            }
        };
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
        SimpleAjaxSubmitLink importButton = new SimpleAjaxSubmitLink("import", importForm);
        importForm.add(importButton);
        return importForm;
    }
}
