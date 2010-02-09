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
package org.artifactory.webapp.wicket.importexport.system;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.artifactory.process.StatusHolder;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.AutoCompletePath;
import org.artifactory.webapp.wicket.components.SimpleAjaxSubmitLink;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;

import java.io.File;
import java.util.Date;

/**
 * @author Yoav Landman
 */
public class ExportSystemPanel extends TitlePanel {

    private final static Logger LOGGER = Logger.getLogger(ExportSystemPanel.class);

    @SuppressWarnings({"UnusedDeclaration"})
    private String exportToPath;

    @SuppressWarnings({"UnusedDeclaration"})
    private boolean createArchive;

    @SuppressWarnings({"ThisEscapedInObjectConstruction"})
    public ExportSystemPanel(String string) {
        super(string);
        final StatusHolder status = new StatusHolder();
        Form exportForm = new Form("exportForm") {
            protected void onSubmit() {
                ArtifactoryContext context = ContextHelper.get();
                try {
                    context.exportTo(new File(exportToPath), createArchive, new Date(), status);
                } catch (Exception e) {
                    error("Failed to export system to '" + exportToPath + "': " + e.getMessage());
                    LOGGER.error("Failed to export system.", e);
                    return;
                } finally {
                    status.setStatus("Idle.");
                }
                File exportFile = (File) status.getCallback();
                info("Successfully exported system to '" + exportFile.getPath() + "'.");
            }
        };
        add(exportForm);
        AutoCompletePath exportToPathTf =
                new AutoCompletePath("exportToPath", new PropertyModel(this, "exportToPath"));
        exportToPathTf.setRequired(true);
        exportForm.add(exportToPathTf);
        CheckBox createArchiveCheckbox =
                new CheckBox("createArchive", new PropertyModel(this, "createArchive"));
        exportForm.add(createArchiveCheckbox);

        SimpleAjaxSubmitLink exportButton = new SimpleAjaxSubmitLink("export", exportForm);
        exportForm.add(exportButton);
        final Label statusLabel = new Label("status");
        statusLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(1000)) {
            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                super.onPostProcessTarget(target);
                statusLabel.setModel(new PropertyModel(status, "status"));
            }
        });
        //exportForm.add(statusLabel);
    }
}