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
package org.artifactory.webapp.wicket.importexport;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.engine.RepoAccessException;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.webapp.wicket.panel.TitlePanel;

import java.io.File;
import java.util.List;

/**
 * @author freddy33
 */
public class BasicImportPanel extends TitlePanel {
    private final static Logger LOGGER = Logger.getLogger(BasicImportPanel.class);

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo targetRepo;
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

    private Form createImportForm() {
        Form importForm = new Form("importForm") {
            protected void onSubmit() {
                //Import each file seperately to avoid a long running transaction
                File folder = new File(importFromPath);
                try {
                    targetRepo.importFromDir(folder, false);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    Throwable t = e.getCause();
                    if (t != null && t instanceof RepoAccessException) {
                        msg = t.getMessage();
                    }
                    String message = "Failed to import '" + importFromPath + "' into '" + targetRepo.getKey() +
                            "': " + msg;
                    error(message);
                    LOGGER.info(message, e);
                    return;
                }
                info("Successfully imported '" + importFromPath + "' into '" + targetRepo.getKey() +
                        "'.");
            }
        };
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final CentralConfig cc = CentralConfig.get();
        final IModel targetRepoModel = new PropertyModel(this, "targetRepo");
        final List<LocalRepo> localRepos = cc.getLocalAndCachedRepositories();
        final LocalRepo defaultTarget = localRepos.get(0);
        DropDownChoice targetRepoDdc =
                new DropDownChoice("targetRepo", targetRepoModel, localRepos) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return defaultTarget.toString();
                    }
                };
        //Needed because get getDefaultChoice does not update the actual selection object
        targetRepoDdc.setModelObject(defaultTarget);
        importForm.add(targetRepoDdc);

        SubmitLink importButton = new SubmitLink("import");
        importForm.add(importButton);
        return importForm;
    }
}
