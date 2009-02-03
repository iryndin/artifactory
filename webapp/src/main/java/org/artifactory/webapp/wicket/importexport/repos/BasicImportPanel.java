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
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.ExceptionUtils;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.webapp.wicket.components.SimpleAjaxSubmitLink;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freddy33
 */
public class BasicImportPanel extends TitlePanel {
    private final static Logger LOGGER = Logger.getLogger(BasicImportPanel.class);

    @SuppressWarnings({"UnusedDeclaration"})
    private String targetRepoKey;
    @SuppressWarnings({"UnusedDeclaration"})
    private String importFromPath;

    private Form importForm;

    public BasicImportPanel(String id) {
        super(id);
        importForm = createImportForm();
    }

    public Form getImportForm() {
        return importForm;
    }

    public String getImportFromPath() {
        return importFromPath;
    }

    public void setImportFromPath(String importFromPath) {
        this.importFromPath = importFromPath;
    }

    @SuppressWarnings({"unchecked"})
    private Form createImportForm() {
        Form importForm = new Form("importForm") {
            protected void onSubmit() {
                CentralConfig cc = CentralConfig.get();
                try {
                    //If we chose "All" import all local repositories, else import a single repo
                    VirtualRepo globalVirtualRepo = cc.getGlobalVirtualRepo();
                    File folder = new File(importFromPath);
                    if (ImportExportReposPage.ALL_REPOS.equals(targetRepoKey)) {
                        //Import the local repositories
                        List<LocalRepo> repoList =
                                globalVirtualRepo.getLocalAndCachedRepositories();
                        for (LocalRepo localRepo : repoList) {
                            localRepo.importFromDir(folder, false, true);
                        }
                    } else {
                        LocalRepo localRepo =
                                globalVirtualRepo.localOrCachedRepositoryByKey(targetRepoKey);
                        //Import each file seperately to avoid a long running transaction
                        localRepo.importFromDir(folder, false, false);

                    }
                } catch (Exception e) {
                    Throwable cause = ExceptionUtils.unwrapThrowablesOfTypes(
                            e, ArtifactDeploymentException.class, RuntimeException.class);
                    String msg = "Failed to import '" + importFromPath + "' into '" +
                            targetRepoKey + "'. Cause: " + cause.getMessage();
                    error(msg);
                    LOGGER.warn(msg, e);
                    return;
                }
                info("Successfully imported '" + importFromPath + "' into '" + targetRepoKey +
                        "'.");
            }
        };
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final CentralConfig cc = CentralConfig.get();
        final IModel targetRepoModel = new PropertyModel(this, "targetRepoKey");
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        final List<LocalRepo> localRepos = virtualRepo.getLocalAndCachedRepositories();
        final List<String> repoKeys = new ArrayList<String>(localRepos.size() + 1);
        //Add the "All" pseudo repository
        repoKeys.add(ImportExportReposPage.ALL_REPOS);
        for (LocalRepo localRepo : localRepos) {
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
