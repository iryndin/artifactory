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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.artifactory.process.StatusHolder;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.ArtifactoryApp;
import org.artifactory.webapp.wicket.components.AutoCompletePath;
import org.artifactory.webapp.wicket.components.SimpleAjaxSubmitLink;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;

import java.io.File;
import java.io.IOException;

/**
 * @author Yoav Landman
 */
public class ImportSystemPanel extends TitlePanel {

    private final static Logger LOGGER = Logger.getLogger(ImportSystemPanel.class);

    @SuppressWarnings({"UnusedDeclaration"})
    private String importFromPath;

    @SuppressWarnings({"ThisEscapedInObjectConstruction"})
    public ImportSystemPanel(String string) {
        super(string);
        final StatusHolder status = new StatusHolder();
        status.setStatus("Idle.");
        Form importForm = new Form("importForm") {
            protected void onSubmit() {
                //If the path denotes an archive extract it first, else use the directory
                de.schlichtherle.io.File file = new de.schlichtherle.io.File(importFromPath);
                File importFromFolder = null;
                try {
                    if (file.isArchive()) {
                        //Extract the archive
                        status.setStatus("Extracting archive...");
                        importFromFolder =
                                new File(ArtifactoryApp.UPLOAD_FOLDER, file.getName() + "_extract");
                        FileUtils.deleteDirectory(importFromFolder);
                        FileUtils.forceMkdir(importFromFolder);
                        file.copyAllTo(importFromFolder);
                    } else if (file.isDirectory()) {
                        importFromFolder = file;
                    } else {
                        error("Failed to import system from '" + importFromPath +
                                "': Unrecognized file type.");
                        return;
                    }
                    status.setStatus("Importing from directory...");
                    ArtifactoryContext context = ContextHelper.get();
                    context.importFrom(importFromFolder.getPath(), status);
                    info("Successfully imported system from '" + importFromPath + "'.");
                } catch (Exception e) {
                    error("Failed to import system from '" + importFromPath + "': " +
                            e.getMessage());
                    LOGGER.error("Failed to import system.", e);
                } finally {
                    if (file.isArchive()) {
                        //Delete the extracted dir
                        try {
                            de.schlichtherle.io.File.umount();
                            FileUtils.deleteDirectory(importFromFolder);
                        } catch (IOException e) {
                            LOGGER.warn(
                                    "Failed to delete export directory: " +
                                            importFromFolder.getAbsolutePath(), e);
                        }
                    }
                    status.reset();
                }
            }
        };
        add(importForm);
        AutoCompletePath importToPathTf =
                new AutoCompletePath("importFromPath", new PropertyModel(this, "importFromPath"));
        importToPathTf.setRequired(true);
        importForm.add(importToPathTf);
        SimpleAjaxSubmitLink importButton = new SimpleAjaxSubmitLink("import", importForm);
        importForm.add(importButton);
        final Label statusLabel = new Label("status");
        statusLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(1000)) {
            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                super.onPostProcessTarget(target);
                statusLabel.setModel(new PropertyModel(status, "status"));
            }
        });
        //importForm.add(statusLabel);
    }
}