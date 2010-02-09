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
package org.artifactory.webapp.wicket.page.importexport.system;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.IAjaxCallDecorator;
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
import org.artifactory.webapp.wicket.common.behavior.AjaxCallConfirmationDecorator;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Yoav Landman
 */
public class ImportSystemPanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(ImportSystemPanel.class);

    @WicketProperty
    private File importFromPath;

    public ImportSystemPanel(String string) {
        super(string);
        final StatusHolder status = new StatusHolder();
        status.setStatus("Idle.", log);
        Form importForm = new Form("importForm");
        add(importForm);
        PropertyModel pathModel = new PropertyModel(this, "importFromPath");
        final PathAutoCompleteTextField importToPathTf =
                new PathAutoCompleteTextField("importFromPath", pathModel);
        importToPathTf.setRequired(true);
        importForm.add(importToPathTf);

        importForm.add(new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(importToPathTf);
            }
        });

        SimpleButton importButton = new SimpleButton("import", importForm, "Import") {
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                String confirmImportMessage =
                        "Full system import will wipe all existing Artifactory content.\\n" +
                                "Are you sure you want to continue?";

                return new AjaxCallConfirmationDecorator(super.getAjaxCallDecorator(), confirmImportMessage);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                //If the path denotes an archive extract it first, else use the directory
                de.schlichtherle.io.File file = new de.schlichtherle.io.File(importFromPath);
                File importFromFolder = null;
                try {
                    if (!importFromPath.exists()) {
                        error("Specified location '" + importFromPath + "' does not exist.");
                        return;
                    }
                    if (importFromPath.isDirectory()) {
                        if (importFromPath.list().length == 0) {
                            error("Directory '" + importFromPath + "' is empty.");
                            return;
                        }
                        importFromFolder = file;

                    } else if (file.isArchive()) {
                        //Extract the archive
                        status.setStatus("Extracting archive...", log);
                        importFromFolder =
                                new File(ArtifactoryHome.getTmpUploadsDir(),
                                        file.getName() + "_extract");
                        FileUtils.deleteDirectory(importFromFolder);
                        FileUtils.forceMkdir(importFromFolder);
                        file.copyAllTo(importFromFolder);
                    } else {
                        error("Failed to import system from '" + importFromPath +
                                "': Unrecognized file type.");
                        return;
                    }
                    status.setStatus("Importing from directory...", log);
                    ArtifactoryContext context = ContextHelper.get();
                    ImportSettings importSettings = new ImportSettings(importFromFolder);
                    // TODO: Add check boxes for these falgs
                    importSettings.setFailFast(false);
                    importSettings.setUseSymLinks(false);
                    importSettings.setFailIfEmpty(true);
                    context.importFrom(importSettings, status);
                    if (status.isError()) {
                        String msg = "Error while importing system from '" + importFromPath +
                                "': " + status.getStatusMsg();
                        error(msg);
                        if (status.getException() != null) {
                            log.warn(msg, status.getException());
                        }
                    } else {
                        info("Successfully imported system from '" + importFromPath + "'.");
                    }
                } catch (Exception e) {
                    error("Failed to import system from '" + importFromPath + "': " +
                            e.getMessage());
                    log.error("Failed to import system.", e);
                } finally {
                    FeedbackUtils.refreshFeedback(target);
                    if (file.isArchive()) {
                        //Delete the extracted dir
                        try {
                            de.schlichtherle.io.File.umount();
                            FileUtils.deleteDirectory(importFromFolder);
                        } catch (IOException e) {
                            log.warn("Failed to delete export directory: " + importFromFolder, e);
                        }
                    }
                    status.reset();
                }
            }
        };
        importForm.add(importButton);
        importForm.add(new DefaultButtonBehavior(importButton));

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