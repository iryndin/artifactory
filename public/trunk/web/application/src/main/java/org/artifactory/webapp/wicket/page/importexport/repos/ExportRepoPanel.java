/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.importexport.repos;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.repo.BackupService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.common.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.common.wicket.component.file.path.PathMask;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ExportRepoPanel extends TitledPanel {
    private static final Logger log = LoggerFactory.getLogger(ExportRepoPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private BackupService backupService;

    @WicketProperty
    private String sourceRepoKey;

    @WicketProperty
    private File exportToPath;

    @WicketProperty
    private boolean m2Compatible;

    @WicketProperty
    private boolean excludeMetadata;

    public ExportRepoPanel(String string) {
        super(string);
        Form exportForm = new Form("exportForm");
        add(exportForm);

        IModel sourceRepoModel = new PropertyModel(this, "sourceRepoKey");
        List<LocalRepoDescriptor> localRepos = repositoryService.getLocalAndCachedRepoDescriptors();
        final List<String> repoKeys = new ArrayList<String>(localRepos.size() + 1);
        //Add the "All" pseudo repository
        repoKeys.add(ImportExportReposPage.ALL_REPOS);
        for (LocalRepoDescriptor localRepo : localRepos) {
            String key = localRepo.getKey();
            repoKeys.add(key);
        }
        DropDownChoice sourceRepoDdc =
                new DropDownChoice("sourceRepo", sourceRepoModel, repoKeys);
        //Needed because get getDefaultChoice does not update the actual selection object
        sourceRepoDdc.setModelObject(ImportExportReposPage.ALL_REPOS);
        exportForm.add(sourceRepoDdc);

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
        exportForm.add(new HelpBubble("m2CompatibleHelp",
                "Include Maven 2 repository metadata and checksum files as part of the export"));
        exportForm.add(new StyledCheckbox("excludeMetadata", new PropertyModel(this, "excludeMetadata")));
        exportForm.add(new HelpBubble("excludeMetadataHelp",
                "Exclude Artifactory-specific metadata from the export.\n" +
                        "(Maven 2 metadata is unaffected by this setting)"));

        TitledAjaxSubmitLink exportButton = new TitledAjaxSubmitLink("export", "Export", exportForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    Session.get().cleanupFeedbackMessages();
                    //If we chose "All" run manual backup to dest dir, else export a single repo
                    MultiStatusHolder status = new MultiStatusHolder();
                    ExportSettings exportSettings = new ExportSettings(exportToPath, status);
                    exportSettings.setIncludeMetadata(!excludeMetadata);
                    exportSettings.setM2Compatible(m2Compatible);
                    if (ImportExportReposPage.ALL_REPOS.equals(sourceRepoKey)) {
                        backupService.backupRepos(exportToPath, exportSettings);
                    } else {
                        repositoryService.exportRepo(sourceRepoKey, exportSettings);
                    }
                    List<StatusEntry> warnings = status.getWarnings();
                    if (!warnings.isEmpty()) {
                        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                        warn(warnings.size() + " Warnings have been produces during the export. Please review the " +
                                "<a href=\"" + systemLogsPage + "\">log</a> for further information.");
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        error("Failed to export from: " + sourceRepoKey + "' to '" + exportToPath + "'. Cause: " +
                                message);
                    } else {
                        info("Successfully exported '" + sourceRepoKey + "' to '" + exportToPath + "'.");
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
}
