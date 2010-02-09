package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.utils.CronUtils;
import org.artifactory.webapp.wicket.utils.validation.CronExpValidator;

import java.util.Date;
import java.util.List;

/**
 * General settings (server name, max upload, etc.) configuration panel.
 *
 * @author Yossi Shaul
 */
class IndexerConfigPanel extends Panel {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;
    private IndexerDescriptor indexer;

    public IndexerConfigPanel(String id, Form form) {
        super(id);
        add(new Label("title", getTitle()));

        TitledBorder border = new TitledBorder("border", "fieldset-border");
        add(border);

        MutableCentralConfigDescriptor centralConfig =
                centralConfigService.getDescriptorForEditing();
        indexer = centralConfig.getIndexer();
        // indexer might be null so create it if it is but not set yet it in the central config. only on submit
        if (indexer == null) {
            indexer = new IndexerDescriptor();
            indexer.setCronExp("");
        }

        setModel(new CompoundPropertyModel(indexer));
        final TextField textField = new TextField("cronExp");
        textField.add(CronExpValidator.getInstance());
        border.add(textField);
        border.add(new SchemaHelpBubble("cronExp.help"));
        String nextRun = "";
        if ((textField.getValue() != null) &&
                (!"".equals(textField.getValue()))) {
            nextRun = getNextRunTime(textField.getValue());
        }
        final Label nextRunLabel = new Label("cronExpNextRun", nextRun);
        nextRunLabel.setOutputMarkupId(true);
        border.add(nextRunLabel);
        SimpleButton calculateButton = new SimpleButton("calculate", form, "Calculate") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                nextRunLabel.setModel(new Model(getNextRunTime(textField.getValue())));
                target.addComponent(nextRunLabel);
            }
        };
        calculateButton.setDefaultFormProcessing(false);
        border.add(calculateButton);
        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        border.add(new ListMultipleChoice("excludedRepositories", repos));
        border.add(new SchemaHelpBubble("excludedRepositories.help"));
    }

    /**
     * @return The indexer edited in this panel.
     */
    public IndexerDescriptor getIndexer() {
        return indexer;
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

    public String getTitle() {
        return "Indexer";
    }
}
