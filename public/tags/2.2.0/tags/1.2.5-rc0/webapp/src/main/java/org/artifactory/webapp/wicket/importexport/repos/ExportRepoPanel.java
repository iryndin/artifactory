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
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.backup.BackupHelper;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.AutoCompletePath;
import org.artifactory.webapp.wicket.panel.TitlePanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ExportRepoPanel extends TitlePanel {

    private final static Logger LOGGER = Logger.getLogger(ExportRepoPanel.class);

    @SuppressWarnings({"UnusedDeclaration"})
    private String sourceRepoKey;
    @SuppressWarnings({"UnusedDeclaration"})
    private String exportToPath;

    public ExportRepoPanel(String string) {
        super(string);
        Form exportForm = new Form("exportForm") {
            protected void onSubmit() {
                CentralConfig cc = CentralConfig.get();
                try {
                    //If we chose "All" run manual backup to dest dir, else export a single repo
                    if (ImportExportReposPage.ALL_REPOS.equals(sourceRepoKey)) {
                        ArtifactoryContext context = ContextHelper.get();
                        BackupHelper backup = context.getBackup();
                        backup.backupRepos(exportToPath);
                    } else {
                        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
                        LocalRepo sourceRepo =
                                virtualRepo.localOrCachedRepositoryByKey(sourceRepoKey);
                        sourceRepo.exportToDir(new File(exportToPath));
                    }
                } catch (Exception e) {
                    String message =
                            "Failed to export '" + sourceRepoKey + "' to '" + exportToPath +
                                    "': " + e.getMessage();
                    error(message);
                    LOGGER.warn(message, e);
                    return;
                }
                info("Successfully exported '" + sourceRepoKey + "' to '" + exportToPath +
                        "'.");
            }
        };
        add(exportForm);
        final IModel sourceRepoModel = new PropertyModel(this, "sourceRepoKey");
        CentralConfig cc = CentralConfig.get();
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        List<LocalRepo> localRepos = virtualRepo.getLocalAndCachedRepositories();
        final List<String> repoKeys = new ArrayList<String>(localRepos.size() + 1);
        //Add the "All" pseudo repository
        repoKeys.add(ImportExportReposPage.ALL_REPOS);
        for (LocalRepo localRepo : localRepos) {
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
        AutoCompletePath exportToPathTf =
                new AutoCompletePath("exportToPath", new PropertyModel(this, "exportToPath"));
        exportToPathTf.setRequired(true);
        exportForm.add(exportToPathTf);
        SubmitLink exportButton = new SubmitLink("export");
        exportForm.add(exportButton);
    }
}
