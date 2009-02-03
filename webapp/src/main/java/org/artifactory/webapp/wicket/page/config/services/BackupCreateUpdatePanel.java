package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.file.path.PathMask;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.utils.CronUtils;
import org.artifactory.webapp.wicket.utils.validation.CronExpValidator;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.utils.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.utils.validation.XsdNCNameValidator;

import java.util.Date;
import java.util.List;

/**
 * Backups configuration panel.
 *
 * @author Yossi Shaul
 */
class BackupCreateUpdatePanel extends CreateUpdatePanel<BackupDescriptor> {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    public BackupCreateUpdatePanel(CreateUpdateAction action, BackupDescriptor backupDescriptor) {

        super(action, backupDescriptor);
        setWidth(400);

        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        // Backup key
        RequiredTextField backupKeyField = new RequiredTextField("key");
        backupKeyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            backupKeyField.add(JcrNameValidator.getInstance());
            backupKeyField.add(XsdNCNameValidator.getInstance());
            backupKeyField.add(new UniqueXmlIdValidator(getEditingDescriptor()));
        }
        border.add(backupKeyField);

        border.add(new StyledCheckbox("enabled"));

        final RequiredTextField cronExpField = new RequiredTextField("cronExp");
        cronExpField.add(CronExpValidator.getInstance());
        border.add(cronExpField);

        final PathAutoCompleteTextField backupDir = new PathAutoCompleteTextField("dir");
        backupDir.setMask(PathMask.FOLDERS);
        border.add(backupDir);

        border.add(new TextField("retentionPeriodHours", Integer.class));
        border.add(new StyledCheckbox("createArchive"));

        // add all the help bubbles
        border.add(new SchemaHelpBubble("key.help"));
        border.add(new SchemaHelpBubble("cronExp.help"));
        border.add(new SchemaHelpBubble("dir.help"));
        border.add(new SchemaHelpBubble("retentionPeriodHours.help"));
        border.add(new SchemaHelpBubble("createArchive.help"));
        border.add(new SchemaHelpBubble("excludedRepositories.help"));

        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        border.add(new ListMultipleChoice("excludedRepositories", repos));
        String nextRun = "";
        if ((cronExpField.getValue() != null) &&
                (!"".equals(cronExpField.getValue()))) {
            nextRun = getNextRunTime(cronExpField.getValue());
        }
        final Label nextRunLabel = new Label("cronExpNextRun", nextRun);
        nextRunLabel.setOutputMarkupId(true);
        border.add(nextRunLabel);
        SimpleButton calculateButton = new SimpleButton("calculate", form, "Calculate") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                nextRunLabel.setModel(new Model(getNextRunTime(cronExpField.getValue())));
                target.addComponent(nextRunLabel);
            }
        };
        calculateButton.setDefaultFormProcessing(false);

        border.add(calculateButton);
        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        SimpleButton submit = createSubmitButton();
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private SimpleButton createSubmitButton() {
        String submitCaption = isCreate() ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
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
                ((ServicesConfigPage) getPage()).refresh(target);
                close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    private String getNextRunTime(String cronExpression) {
        String nextRunLabelValue = "The expression is not valid.";
        if (CronUtils.isValid(cronExpression)) {
            Date nextExecution = CronUtils.getNextExecution(cronExpression);
            nextRunLabelValue = formatDate(nextExecution);
        }
        return nextRunLabelValue;
    }

    private String formatDate(Date nextRunDate) {
        return nextRunDate.toString();
    }

    protected MutableCentralConfigDescriptor getEditingDescriptor() {
        return centralConfigService.getDescriptorForEditing();
    }
}