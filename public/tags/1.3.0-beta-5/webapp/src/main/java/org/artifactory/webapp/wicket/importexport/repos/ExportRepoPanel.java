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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.repo.BackupService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.component.SimpleAjaxSubmitLink;
import org.artifactory.webapp.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.component.file.path.PathMask;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ExportRepoPanel extends TitledPanel {

    private final static Logger LOGGER = Logger.getLogger(ExportRepoPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private BackupService backupService;

    @WicketProperty
    private String sourceRepoKey;

    @WicketProperty
    private File exportToPath;

    public ExportRepoPanel(String string) {
        super(string);
        Form exportForm = new Form("exportForm") {
            @Override
            protected void onSubmit() {
                try {
                    //If we chose "All" run manual backup to dest dir, else export a single repo
                    if (ImportExportReposPage.ALL_REPOS.equals(sourceRepoKey)) {
                        backupService.backupRepos(exportToPath);
                    } else {
                        final StatusHolder status = new StatusHolder();
                        ExportSettings exportSettings = new ExportSettings(exportToPath);
                        repositoryService.exportRepo(sourceRepoKey, exportSettings, status);
                    }
                } catch (Exception e) {
                    String message =
                            "Failed to export '" + sourceRepoKey + "' to '" + exportToPath +
                                    "': " + e.getMessage();
                    error(message);
                    LOGGER.warn(message, e);
                    return;
                }
                info("Successfully exported '" + sourceRepoKey + "' to '" + exportToPath + "'.");
            }
        };
        add(exportForm);
        final IModel sourceRepoModel = new PropertyModel(this, "sourceRepoKey");
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
        final PathAutoCompleteTextField exportToPathTf = new PathAutoCompleteTextField("exportToPath", pathModel);
        exportToPathTf.setMask(PathMask.FOLDERS);
        exportToPathTf.setRequired(true);

        FileBrowserButton browserButton = new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(exportToPathTf);
            }
        };
        browserButton.setMask(PathMask.FOLDERS);
        exportForm.add(browserButton);

        exportToPathTf.setRequired(true);
        exportForm.add(exportToPathTf);
        AjaxSubmitLink exportButton = new SimpleAjaxSubmitLink("export", exportForm);
        exportForm.add(exportButton);
    }
}
