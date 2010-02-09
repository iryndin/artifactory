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

import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.webapp.wicket.panel.WindowPanel;
import org.artifactory.webapp.wicket.widget.AutoCompletePath;
import wicket.markup.html.form.DropDownChoice;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.SubmitLink;
import wicket.model.IModel;
import wicket.model.PropertyModel;

import java.io.File;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ImportPanel extends WindowPanel {

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo targetRepo;
    @SuppressWarnings({"UnusedDeclaration"})
    private String importFromPath;

    public ImportPanel(String string) {
        super(string);
        Form importForm = new Form("importForm") {
            protected void onSubmit() {
                //Import each file seperately to avoid a long running transaction
                File folder = new File(importFromPath);
                targetRepo.importFromDir(folder, false);
            }
        };

        importForm.setOutputMarkupId(false);
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final CentralConfig cc = getCc();
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
        AutoCompletePath importFromPathTf =
                new AutoCompletePath("importFromPath", new PropertyModel(this, "importFromPath"));
        importFromPathTf.setRequired(true);
        //--importFromPath.add(new ValidateArtifactFormBehavior("onKeyup"));
        importForm.add(importFromPathTf);
        //Add the import button
        /*Link importButton = new AjaxFallbackLink("import") {
            public void onClick(final AjaxRequestTarget target) {
               targetRepo.importFolder(new File(importFromPath));
            }
        };*/
        SubmitLink importButton = new SubmitLink("import");
        importForm.add(importButton);
    }
}
