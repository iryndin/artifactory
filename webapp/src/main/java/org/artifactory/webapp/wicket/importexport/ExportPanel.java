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
public class ExportPanel extends WindowPanel {

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo sourceRepo;
    @SuppressWarnings({"UnusedDeclaration"})
    private String exportToPath;

    public ExportPanel(String string) {
        super(string);
        Form exportForm = new Form("exportForm") {
            protected void onSubmit() {
                sourceRepo.exportToDir(new File(exportToPath));
            }
        };
        exportForm.setOutputMarkupId(false);
        add(exportForm);
        final IModel sourceRepoModel = new PropertyModel(this, "sourceRepo");
        final CentralConfig cc = getCc();
        final List<LocalRepo> localRepos = cc.getLocalAndCachedRepositories();

        final LocalRepo defaultSource = localRepos.get(0);
        DropDownChoice sourceRepoDdc =
                new DropDownChoice("sourceRepo", sourceRepoModel, localRepos) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return defaultSource.toString();
                    }
                };
        //Needed because get getDefaultChoice does not update the actual selection object
        sourceRepoDdc.setModelObject(defaultSource);
        exportForm.add(sourceRepoDdc);
        AutoCompletePath exportToPathTf =
                new AutoCompletePath("exportToPath", new PropertyModel(this, "exportToPath"));
        exportToPathTf.setRequired(true);
        //--importFromPath.add(new ValidateArtifactFormBehavior("onKeyup"));
        exportForm.add(exportToPathTf);
        //Add the import button
        /*Link importButton = new AjaxFallbackLink("import") {
            public void onClick(final AjaxRequestTarget target) {
               targetRepo.importFolder(new File(importFromPath));
            }
        };*/
        SubmitLink exportButton = new SubmitLink("export");
        exportForm.add(exportButton);
    }
}
