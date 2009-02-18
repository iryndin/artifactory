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
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.dnd.select.sorted.SortedDragDropSelection;
import org.artifactory.webapp.wicket.common.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.file.path.PathMask;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.services.cron.CronNextDatePanel;
import org.artifactory.webapp.wicket.utils.validation.CronExpValidator;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.utils.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.utils.validation.XsdNCNameValidator;

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

    public BackupCreateUpdatePanel(CreateUpdateAction action, BackupDescriptor backupDescriptor) {
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
            keyField.add(new UniqueXmlIdValidator(getEditingDescriptor()));
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

        advancedFields.add(new StyledCheckbox("createArchive"));
        advancedFields.add(new SchemaHelpBubble("createArchive.help"));

        createIncremental = new StyledCheckbox("createIncrementalBackup",
                new PropertyModel(this, "createIncrementalBackup"));
        createIncremental.setOutputMarkupId(true);
        createIncremental.setRequired(false);
        createIncremental.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                retentionHoursField.setEnabled(!createIncremental.isChecked());
                if (createIncremental.isChecked()) {
                    retentionHoursField.setModelObject("0");
                }
                target.addComponent(retentionHoursField);
            }
        });
        advancedFields.add(createIncremental);

        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        advancedFields.add(new SortedDragDropSelection<RepoDescriptor>("excludedRepositories", repos));
        advancedFields.add(new SchemaHelpBubble("excludedRepositories.help"));

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        SimpleButton submit = createSubmitButton();
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private SimpleButton createSubmitButton() {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new SimpleButton("submit", form, submitCaption) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (isCreate()) {
                    getEditingDescriptor().addBackup(entity);
                    centralConfigService.saveEditedDescriptorAndReload();
                    getPage().info("Backup '" + entity.getKey() + "' successfully created.");
                } else {
                    centralConfigService.saveEditedDescriptorAndReload();
                    getPage().info("Backup '" + entity.getKey() + "' successfully updated.");
                }
                FeedbackUtils.refreshFeedback(target);
                ((BackupsListPage) getPage()).refresh(target);
                close(target);
            }
        };
    }

    protected MutableCentralConfigDescriptor getEditingDescriptor() {
        return centralConfigService.getDescriptorForEditing();
    }

}
