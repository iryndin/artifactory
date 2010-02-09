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

package org.artifactory.webapp.wicket.panel.export;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.common.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.common.wicket.component.file.path.PathMask;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

/**
 * @author Tomer Cohen
 */
public class ExportResultsPanel extends FieldSetPanel {

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private RepositoryService repoService;

    @WicketProperty
    private String searchResultName;

    @WicketProperty
    private File exportToPath;

    @WicketProperty
    private boolean m2Compatible;

    @WicketProperty
    private boolean excludeMetadata;

    @WicketProperty
    private boolean createArchive;

    private static final Logger log = LoggerFactory.getLogger(ExportResultsPanel.class);

    public ExportResultsPanel(String id, final ActionableItem actionableItem) {
        super(id, new Model("Export to Path"));
        searchResultName = actionableItem.getDisplayName();
        Form exportForm = new Form("exportForm");
        add(exportForm);
        PropertyModel pathModel = new PropertyModel(this, "exportToPath");
        final PathAutoCompleteTextField exportToPathTf =
                new PathAutoCompleteTextField("exportToPath", pathModel);
        exportToPathTf.setMask(PathMask.FOLDERS);
        exportToPathTf.setRequired(true);
        exportForm.add(exportToPathTf);

        FileBrowserButton browserButton = new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(exportToPathTf);
            }
        };
        browserButton.setMask(PathMask.FOLDERS);
        exportForm.add(browserButton);

        exportForm.add(new StyledCheckbox("m2Compatible", new PropertyModel(this, "m2Compatible")));
        exportForm.add(new HelpBubble("m2CompatibleHelp", new ResourceModel("m2CompatibleHelp")));
        exportForm.add(new StyledCheckbox("excludeMetadata", new PropertyModel(this, "excludeMetadata")));
        exportForm.add(new HelpBubble("excludeMetadataHelp", new ResourceModel("excludeMetadataHelp")));
        exportForm.add(new StyledCheckbox("createArchive", new PropertyModel(this, "createArchive")));
        exportForm.add(new HelpBubble("createArchiveHelp", new ResourceModel("createArchiveHelp")));
        TitledAjaxSubmitLink exportButton = new TitledAjaxSubmitLink("export", "Export", exportForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    Session.get().cleanupFeedbackMessages();
                    exportToPath = new File(exportToPathTf.getModelObjectAsString());
                    List<FileInfo> searchResults = ArtifactoryWebSession.get().getResults(searchResultName);
                    MultiStatusHolder status = repoService
                            .exportSearchResults(searchResults, exportToPath, !excludeMetadata, m2Compatible,
                                    createArchive);
                    List<StatusEntry> warnings = status.getWarnings();
                    if (!warnings.isEmpty()) {
                        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                        warn(warnings.size() + " warning(s) reported during the export. Please review the " +
                                "<a href=\"" + systemLogsPage + "\">log</a> for further information.");
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        error("Failed to export from: " + searchResultName + "' to '" + exportToPath + "'. Cause: " +
                                message);
                    } else {
                        info("Successfully exported '" + searchResultName + "' to '" + exportToPath + "'.");
                    }
                } catch (Exception e) {
                    String message = "Exception occurred during export: ";
                    error(message + e.getMessage());
                    log.error(message, e);
                }
                AjaxUtils.refreshFeedback(target);
                target.addComponent(form);
            }
        };
        exportForm.add(exportButton);
        exportForm.add(new DefaultButtonBehavior(exportButton));
    }


    @Override
    public String getTitle() {
        return "Export Search Results";
    }
}