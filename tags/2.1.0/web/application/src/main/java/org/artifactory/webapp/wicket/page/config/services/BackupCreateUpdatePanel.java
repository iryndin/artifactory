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

package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.common.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.common.wicket.component.file.path.PathMask;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.components.SortedRepoDragDropSelection;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.services.cron.CronNextDatePanel;
import org.artifactory.webapp.wicket.util.validation.CronExpValidator;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;

import java.util.List;

/**
 * Backups configuration panel.
 *
 * @author Yossi Shaul
 */
public class BackupCreateUpdatePanel extends CreateUpdatePanel<BackupDescriptor> {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    @WicketProperty
    private boolean createIncrementalBackup;

    final TextField retentionHoursField;

    final StyledCheckbox createIncremental;
    private StyledCheckbox createArchiveCheckbox;

    public BackupCreateUpdatePanel(CreateUpdateAction action, BackupDescriptor backupDescriptor,
            BackupsListPanel backupsListPanel) {
        super(action, backupDescriptor);
        createIncrementalBackup = backupDescriptor.isIncremental();
        setWidth(550);

        add(form);

        TitledBorder simpleFields = new TitledBorder("simple");
        form.add(simpleFields);

        // Backup key
        RequiredTextField keyField = new RequiredTextField("key");
        keyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            keyField.add(new JcrNameValidator("Invalid backup key '%s'"));
            keyField.add(new XsdNCNameValidator("Invalid backup key '%s'"));
            keyField.add(new UniqueXmlIdValidator(backupsListPanel.getMutableDescriptor()));
        }
        simpleFields.add(keyField);
        simpleFields.add(new SchemaHelpBubble("key.help"));

        simpleFields.add(new StyledCheckbox("enabled"));

        final RequiredTextField cronExpField = new RequiredTextField("cronExp");
        cronExpField.add(CronExpValidator.getInstance());
        simpleFields.add(cronExpField);
        simpleFields.add(new SchemaHelpBubble("cronExp.help"));

        simpleFields.add(new CronNextDatePanel("cronNextDatePanel", cronExpField));

        PropertyModel pathModel = new PropertyModel(backupDescriptor, "dir");

        final PathAutoCompleteTextField backupDir = new PathAutoCompleteTextField("dir", pathModel);
        backupDir.setMask(PathMask.FOLDERS);
        simpleFields.add(backupDir);
        simpleFields.add(new SchemaHelpBubble("dir.help"));


        FileBrowserButton browserButton = new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(backupDir);
            }
        };
        simpleFields.add(browserButton);

        TitledBorder advancedFields = new TitledBorder("advanced");
        form.add(advancedFields);

        retentionHoursField = new TextField("retentionPeriodHours", Integer.class);
        retentionHoursField.setOutputMarkupId(true);
        advancedFields.add(retentionHoursField);
        advancedFields.add(new SchemaHelpBubble("retentionPeriodHours.help"));

        createArchiveCheckbox = new StyledCheckbox("createArchive");
        createArchiveCheckbox.setOutputMarkupId(true);
        createArchiveCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean isCreateArchiveChecked = createArchiveCheckbox.isChecked();
                createIncremental.setEnabled(!isCreateArchiveChecked);
                if (isCreateArchiveChecked) {
                    createIncremental.setModelObject(Boolean.FALSE);
                }
                target.addComponent(createIncremental);
            }
        });
        advancedFields.add(createArchiveCheckbox);
        advancedFields.add(new SchemaHelpBubble("createArchive.help"));

        advancedFields.add(new StyledCheckbox("sendMailOnError"));
        advancedFields.add(new SchemaHelpBubble("sendMailOnError.help"));

        createIncremental = new StyledCheckbox("createIncrementalBackup",
                new PropertyModel(this, "createIncrementalBackup"));
        createIncremental.setOutputMarkupId(true);
        createIncremental.setRequired(false);
        createIncremental.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean isIncrementalChecked = createIncremental.isChecked();
                createArchiveCheckbox.setEnabled(!isIncrementalChecked);
                retentionHoursField.setEnabled(!isIncrementalChecked);
                if (isIncrementalChecked) {
                    createArchiveCheckbox.setModelObject(Boolean.FALSE);
                    retentionHoursField.setModelObject("0");
                }
                target.addComponent(retentionHoursField);
                target.addComponent(createArchiveCheckbox);
            }
        });
        advancedFields.add(createIncremental);

        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        advancedFields.add(new SortedRepoDragDropSelection<RepoDescriptor>("excludedRepositories", repos));
        advancedFields.add(new SchemaHelpBubble("excludedRepositories.help"));

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        TitledAjaxSubmitLink submit = createSubmitButton(backupsListPanel);
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private TitledAjaxSubmitLink createSubmitButton(final BackupsListPanel backupsListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                MutableCentralConfigDescriptor configDescriptor = backupsListPanel.getMutableDescriptor();
                if (isCreate()) {
                    configDescriptor.addBackup(entity);
                    centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
                    getPage().info("Backup '" + entity.getKey() + "' successfully created.");
                } else {
                    centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
                    getPage().info("Backup '" + entity.getKey() + "' successfully updated.");
                }
                AjaxUtils.refreshFeedback(target);
                target.addComponent(backupsListPanel);
                close(target);
            }
        };
    }
}
