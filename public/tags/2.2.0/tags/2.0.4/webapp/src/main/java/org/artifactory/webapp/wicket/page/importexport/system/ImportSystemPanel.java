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
import org.apache.commons.lang.SystemUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.behavior.AjaxCallConfirmationDecorator;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.codehaus.plexus.util.Expand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class ImportSystemPanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(ImportSystemPanel.class);

    @WicketProperty
    private File importFromPath;

    @WicketProperty
    private boolean copyFiles;

    @WicketProperty
    private boolean useSymLinks;

    @WicketProperty
    private boolean verbose;

    @WicketProperty
    private boolean includeMetadata;

    final StyledCheckbox copyCheckbox;
    final StyledCheckbox symLinkCheckbox;

    public ImportSystemPanel(String string) {
        super(string);
        copyCheckbox = new StyledCheckbox("copyFiles", new PropertyModel(this, "copyFiles"));
        copyCheckbox.setOutputMarkupId(true);

        symLinkCheckbox = new StyledCheckbox("useSymLinks", new PropertyModel(this, "useSymLinks"));
        symLinkCheckbox.setOutputMarkupId(true);

        final MultiStatusHolder status = new MultiStatusHolder();
        status.setStatus("Idle.", log);
        Form importForm = new Form("importForm");
        add(importForm);
        PropertyModel pathModel = new PropertyModel(this, "importFromPath");
        final PathAutoCompleteTextField importToPathTf = new PathAutoCompleteTextField("importFromPath", pathModel);
        importToPathTf.setRequired(true);
        importToPathTf.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            protected void onUpdate(AjaxRequestTarget target) {
                updateCheckboxes(target);
            }
        });
        importForm.add(importToPathTf);

        importForm.add(new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(importToPathTf);
                updateCheckboxes(target);
            }
        });

        StyledCheckbox verboseCheckbox = new StyledCheckbox("verbose", new PropertyModel(this, "verbose"));
        verboseCheckbox.setRequired(false);
        importForm.add(verboseCheckbox);
        importForm.add(new HelpBubble("verboseHelp", "HINT: You can monitor the log in the 'System Logs' page."));

        importForm.add(new StyledCheckbox("includeMetadata", new PropertyModel(this, "includeMetadata")));
        importForm.add(new HelpBubble("includeMetadataHelp",
                "Include Artifactory-specific metadata as part of the export."));

        copyCheckbox.setEnabled(false);
        copyCheckbox.setRequired(false);
        copyCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            protected void onUpdate(AjaxRequestTarget target) {
                if (copyCheckbox.isChecked() && canUseSymLink()) {
                    symLinkCheckbox.setEnabled(true);
                } else {
                    symLinkCheckbox.setEnabled(false);
                    symLinkCheckbox.setModelObject(Boolean.FALSE);
                }
                target.addComponent(symLinkCheckbox);
            }
        });
        importForm.add(copyCheckbox);

        symLinkCheckbox.setOutputMarkupId(true);
        symLinkCheckbox.setEnabled(false);
        symLinkCheckbox.setRequired(false);
        importForm.add(symLinkCheckbox);

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
                status.reset();
                //If the path denotes an archive extract it first, else use the directory
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
                        importFromFolder = importFromPath;
                    } else if (isZip(importFromPath)) {
                        //Extract the archive
                        status.setStatus("Extracting archive...", log);
                        importFromFolder =
                                new File(ArtifactoryHome.getTmpUploadsDir(),
                                        importFromPath.getName() + "_extract");
                        FileUtils.deleteDirectory(importFromFolder);
                        FileUtils.forceMkdir(importFromFolder);
                        //TODO: [by YS] move to ZipUtils
                        try {
                            Expand expand = new Expand();
                            expand.setSrc(importFromPath);
                            expand.setDest(importFromFolder);
                            expand.execute();
                        } catch (Exception e) {
                            String message = "Failed to extract file " + importFromPath.getAbsolutePath();
                            error(message);
                            log.error(message, e);
                            return;
                        }
                    } else {
                        error("Failed to import system from '" + importFromPath +
                                "': Unrecognized file type.");
                        return;
                    }
                    status.setStatus("Importing from directory...", log);
                    ArtifactoryContext context = ContextHelper.get();
                    ImportSettings importSettings = new ImportSettings(importFromFolder);
                    importSettings.setFailFast(false);
                    importSettings.setCopyToWorkingFolder(copyFiles);
                    importSettings.setUseSymLinks(useSymLinks);
                    importSettings.setFailIfEmpty(true);
                    importSettings.setVerbose(verbose);
                    importSettings.setIncludeMetadata(includeMetadata);
                    context.importFrom(importSettings, status);
                    List<StatusEntry> warnings = status.getWarnings();
                    if (!warnings.isEmpty()) {
                        warn(warnings.size() + " Warnings have been produces during the export. " +
                                "Please review the log for further information.");
                    }
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
                    error("Failed to import system from '" + importFromPath + "': " + e.getMessage());
                    log.error("Failed to import system.", e);
                } finally {
                    FeedbackUtils.refreshFeedback(target);
                    if (isZip(importFromPath)) {
                        //Delete the extracted dir
                        try {
                            if (importFromFolder != null) {
                                FileUtils.deleteDirectory(importFromFolder);
                            }
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

    private boolean isZip(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(".zip");
    }

    private void updateCheckboxes(AjaxRequestTarget target) {
        if ((importFromPath != null) && importFromPath.exists()) {
            copyCheckbox.setEnabled(true);
        } else {
            copyCheckbox.setEnabled(false);
            copyCheckbox.setModelObject(Boolean.FALSE);
            symLinkCheckbox.setEnabled(false);
            symLinkCheckbox.setModelObject(Boolean.FALSE);
        }
        target.addComponent(copyCheckbox);
        target.addComponent(symLinkCheckbox);
    }

    private boolean canUseSymLink() {
        boolean isArchive = new de.schlichtherle.io.File(importFromPath).isArchive();
        boolean usingWindows = SystemUtils.IS_OS_WINDOWS;
        return (!isArchive && !usingWindows);
    }
}