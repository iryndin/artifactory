package org.artifactory.webapp.wicket.page.config.services;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
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
import org.artifactory.webapp.wicket.common.component.dnd.select.DragDropSelection;
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
public class BackupCreateUpdatePanel extends CreateUpdatePanel<BackupDescriptor> {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    public BackupCreateUpdatePanel(CreateUpdateAction action, BackupDescriptor backupDescriptor) {

        super(action, backupDescriptor);
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

        PathAutoCompleteTextField backupDir = new PathAutoCompleteTextField("dir");
        backupDir.setMask(PathMask.FOLDERS);
        simpleFields.add(backupDir);
        simpleFields.add(new SchemaHelpBubble("dir.help"));

        TitledBorder advancedFields = new TitledBorder("advanced");
        form.add(advancedFields);

        advancedFields.add(new TextField("retentionPeriodHours", Integer.class));
        advancedFields.add(new SchemaHelpBubble("retentionPeriodHours.help"));

        advancedFields.add(new StyledCheckbox("createArchive"));
        advancedFields.add(new SchemaHelpBubble("createArchive.help"));

        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        advancedFields.add(new DragDropSelection<RepoDescriptor>("excludedRepositories", repos));
        advancedFields.add(new SchemaHelpBubble("excludedRepositories.help"));

        String nextRun = getNextRunTime(cronExpField.getValue());
        final Label nextRunLabel = new Label("cronExpNextRun", nextRun);
        nextRunLabel.setOutputMarkupId(true);
        simpleFields.add(nextRunLabel);

        //TODO: [by yl] Scheduled run calculation button can easily be made a component
        SimpleButton calculateButton = new SimpleButton("calculate", form, "Refresh") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                nextRunLabel.setModel(new Model(getNextRunTime(cronExpField.getValue())));
                target.addComponent(nextRunLabel);
            }
        };
        calculateButton.setDefaultFormProcessing(false);

        simpleFields.add(calculateButton);
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

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
    }

    private String getNextRunTime(String cronExpression) {
        if (StringUtils.isEmpty(cronExpression)) {
            return "The cron expression is blank.";
        }
        if (CronUtils.isValid(cronExpression)) {
            Date nextExecution = CronUtils.getNextExecution(cronExpression);
            return formatDate(nextExecution);
        }
        return "The cron expression is invalid.";
    }

    private String formatDate(Date nextRunDate) {
        return nextRunDate.toString();
    }

    protected MutableCentralConfigDescriptor getEditingDescriptor() {
        return centralConfigService.getDescriptorForEditing();
    }
}