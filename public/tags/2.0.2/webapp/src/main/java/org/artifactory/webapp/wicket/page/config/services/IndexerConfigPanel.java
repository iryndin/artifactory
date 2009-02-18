package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.common.component.CancelButton;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.dnd.select.sorted.SortedDragDropSelection;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.services.cron.CronNextDatePanel;
import org.artifactory.webapp.wicket.utils.validation.CronExpValidator;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * General settings (server name, max upload, etc.) configuration panel.
 *
 * @author Yossi Shaul
 */
public class IndexerConfigPanel extends TitledActionPanel {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;
    private IndexerDescriptor indexer;

    public IndexerConfigPanel(String id, Form form) {
        super(id);

        MutableCentralConfigDescriptor centralConfig =
                centralConfigService.getDescriptorForEditing();
        indexer = centralConfig.getIndexer();
        // indexer might be null so create it if it is but not set yet it in the central config. only on submit
        if (indexer == null) {
            indexer = new IndexerDescriptor();
            indexer.setCronExp("");
        }

        setModel(new CompoundPropertyModel(indexer));
        final TextField cronExpField = new TextField("cronExp");
        cronExpField.add(CronExpValidator.getInstance());
        add(cronExpField);
        add(new SchemaHelpBubble("cronExp.help"));

        add(new CronNextDatePanel("cronNextDatePanel", cronExpField));

        List<RepoDescriptor> repos = repositoryService.getLocalAndRemoteRepoDescriptors();
        add(new SortedDragDropSelection<RepoDescriptor>("excludedRepositories", repos));
        add(new SchemaHelpBubble("excludedRepositories.help"));

        addDefaultButton(createSaveButton(form));
        addButton(new CancelButton(form));
    }

    /**
     * @return The indexer edited in this panel.
     */
    public IndexerDescriptor getIndexer() {
        return indexer;
    }

    private SimpleButton createSaveButton(Form form) {
        return new SimpleButton("save", form, "Save") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // get the indexer configuration and if valid
                IndexerDescriptor indexer = getIndexer();
                MutableCentralConfigDescriptor ccDescriptor = centralConfigService.getDescriptorForEditing();
                if (StringUtils.hasLength(indexer.getCronExp())) {
                    ccDescriptor.setIndexer(indexer);
                } else {
                    // clear the indexer config
                    ccDescriptor.setIndexer(null);
                }
                centralConfigService.saveEditedDescriptorAndReload();
                info("Services settings successfully updated.");
                FeedbackUtils.refreshFeedback(target);
                target.addComponent(this);
            }
        };
    }
}
