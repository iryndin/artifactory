/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.importexport.system;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.common.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ZipUtils;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * @author Yoav Landman
 */
public class ImportSystemPanel extends TitledPanel {
    private static final Logger log = LoggerFactory.getLogger(ImportSystemPanel.class);

    @SpringBean
    private SearchService searchService;

    @WicketProperty
    private File importFromPath;

    @WicketProperty
    private boolean verbose;

    @WicketProperty
    private boolean excludeMetadata;

    @WicketProperty
    private boolean excludeContent;

    @WicketProperty
    private boolean trustServerChecksums;

    public ImportSystemPanel(String string) {
        super(string);

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

        addExcludeMetadataCheckbox(importForm);
        addTrustServerChecksumsCheckbox(importForm);
        addVerboseCheckbox(importForm);

        addImportButton(importForm);
    }

    private void addTrustServerChecksumsCheckbox(Form form) {
        form.add(new StyledCheckbox("trustServerChecksums", new PropertyModel(this, "trustServerChecksums")));
        form.add(new HelpBubble("trustServerChecksumsHelp",
                "Ignore missing checksum and calculate them automatically."));
    }

    private void addVerboseCheckbox(Form importForm) {
        StyledCheckbox verboseCheckbox = new StyledCheckbox("verbose", new PropertyModel(this, "verbose"));
        verboseCheckbox.setRequired(false);
        importForm.add(verboseCheckbox);
        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
        importForm.add(new HelpBubble("verboseHelp",
                "HINT: You can monitor the log in the <a href=\"" + systemLogsPage + "\">'System Logs'</a> page."));
    }

    private void addExcludeMetadataCheckbox(Form importForm) {
        final StyledCheckbox excludeMetadataCheckbox =
                new StyledCheckbox("excludeMetadata", new PropertyModel(this, "excludeMetadata"));
        excludeMetadataCheckbox.setOutputMarkupId(true);
        importForm.add(excludeMetadataCheckbox);
        importForm.add(new HelpBubble("excludeMetadataHelp",
                "Exclude Artifactory-specific metadata from the import."));

        final StyledCheckbox excludeContentCheckbox =
                new StyledCheckbox("excludeContent", new PropertyModel(this, "excludeContent"));
        excludeContentCheckbox.setOutputMarkupId(true);
        excludeContentCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean excludeMDSelected = excludeMetadataCheckbox.isChecked();
                boolean excludeContentSelected = excludeContentCheckbox.isChecked();
                if (excludeMDSelected != excludeContentSelected) {
                    excludeMetadataCheckbox.setModelObject(excludeContentSelected);
                    target.addComponent(excludeMetadataCheckbox);
                }
            }
        });
        importForm.add(excludeContentCheckbox);
        importForm.add(new HelpBubble("excludeContentHelp",
                "Exclude repository content from the import.\n" + "(Import only settings)"));
    }

    private void addImportButton(final Form importForm) {
        TitledAjaxSubmitLink importButton = new TitledAjaxSubmitLink("import", "Import", importForm) {
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                String confirmImportMessage =
                        "Full system import will wipe all existing Artifactory content.\n" +
                                "Are you sure you want to continue?";

                return new ConfirmationAjaxCallDecorator(super.getAjaxCallDecorator(),
                        confirmImportMessage);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                Session.get().cleanupFeedbackMessages();
                MultiStatusHolder status = new MultiStatusHolder();
                //If the path denotes an archive extract it first, else use the directory
                File importFromFolder = null;
                try {
                    if (!importFromPath.exists()) {
                        error("Specified location '" + importFromPath +
                                "' does not exist.");
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
                        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
                        importFromFolder =
                                new File(artifactoryHome.getTmpUploadsDir(),
                                        importFromPath.getName() + "_extract");
                        FileUtils.deleteDirectory(importFromFolder);
                        FileUtils.forceMkdir(importFromFolder);
                        try {
                            ZipUtils.extract(importFromPath, importFromFolder);
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
                    ImportSettings importSettings = new ImportSettings(importFromFolder, status);
                    importSettings.setFailFast(false);
                    importSettings.setFailIfEmpty(true);
                    importSettings.setVerbose(verbose);
                    importSettings.setIncludeMetadata(!excludeMetadata);
                    importSettings.setExcludeContent(excludeContent);
                    importSettings.setTrustServerChecksums(trustServerChecksums);
                    context.importFrom(importSettings);
                    List<StatusEntry> warnings = status.getWarnings();
                    if (!warnings.isEmpty()) {
                        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                        warn(warnings.size() + " Warnings have been produces during the export. Please " +
                                "review the <a href=\"" + systemLogsPage +
                                "\">log</a> for further information.");
                    }
                    if (status.isError()) {
                        String msg =
                                "Error while importing system from '" + importFromPath +
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
                    AjaxUtils.refreshFeedback(target);
                    if (isZip(importFromPath)) {
                        //Delete the extracted dir
                        try {
                            if (importFromFolder != null) {
                                FileUtils.deleteDirectory(importFromFolder);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to delete export directory: " +
                                    importFromFolder, e);
                        }
                    }
                    status.reset();
                    searchService.asyncIndexMarkedArchives();
                }
            }
        };
        importForm.add(importButton);
        importForm.add(new DefaultButtonBehavior(importButton));
    }

    private boolean isZip(File file) {
        return file.isFile() && file.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip");
    }
}