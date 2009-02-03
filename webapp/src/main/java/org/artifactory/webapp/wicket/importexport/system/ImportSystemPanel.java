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
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.component.SimpleAjaxSubmitLink;
import org.artifactory.webapp.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

import java.io.File;
import java.io.IOException;

/**
 * @author Yoav Landman
 */
public class ImportSystemPanel extends TitledPanel {

    private final static Logger LOGGER = Logger.getLogger(ImportSystemPanel.class);

    @WicketProperty
    private File importFromPath;

    public ImportSystemPanel(String string) {
        super(string);
        final StatusHolder status = new StatusHolder();
        status.setStatus("Idle.");
        Form importForm = new Form("importForm") {
            @Override
            protected void onSubmit() {
                //If the path denotes an archive extract it first, else use the directory
                de.schlichtherle.io.File file = new de.schlichtherle.io.File(importFromPath);
                File importFromFolder = null;
                try {
                    if (file.isArchive()) {
                        //Extract the archive
                        status.setStatus("Extracting archive...");
                        importFromFolder =
                                new File(ArtifactoryHome.getTmpUploadsDir(),
                                        file.getName() + "_extract");
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
                    ImportSettings importSettings = new ImportSettings(importFromFolder);
                    context.importFrom(importSettings, status);
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
                            LOGGER.warn("Failed to delete export directory: " + importFromFolder,
                                    e);
                        }
                    }
                    status.reset();
                }
            }
        };
        add(importForm);
        PropertyModel pathModel = new PropertyModel(this, "importFromPath");
        final PathAutoCompleteTextField importToPathTf = new PathAutoCompleteTextField("importFromPath", pathModel);
        importToPathTf.setRequired(true);

        importForm.add(new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(importToPathTf);
            }
        });
        importToPathTf.setRequired(true);
        importForm.add(importToPathTf);

        SimpleAjaxSubmitLink importButton = new SimpleAjaxSubmitLink("import", importForm) {
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {
                    @Override
                    public CharSequence preDecorateScript(CharSequence script) {
                        String confirmImportMessage =
                                "Full system import will wipe all existing Artifactory content.\\n" +
                                        "Are you sure you want to continue?";

                        return "if (!confirm('" + confirmImportMessage + "')) return false;" +
                                script;
                    }
                };
            }
        };
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