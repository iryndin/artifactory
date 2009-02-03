package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.webapp.wicket.common.component.CancelButton;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;
import org.springframework.util.StringUtils;

/**
 * Services configuration panel.
 *
 * @author Yossi Shaul
 */
class ServicesConfigPanel extends TitledActionPanel {

    @SpringBean
    private CentralConfigService centralConfigService;
    private IndexerConfigPanel indexerConfigPanel;

    public ServicesConfigPanel(Form form) {
        super("mainConfigPanel");
        setOutputMarkupId(true);

        indexerConfigPanel = new IndexerConfigPanel("indexerConfigPanel", form);
        add(indexerConfigPanel);
        add(new BackupsListPanel("backupsListPanel"));

        addDefaultButton(createSaveButton(form));
        addButton(new CancelButton(form));
    }

    private SimpleButton createSaveButton(Form form) {
        SimpleButton submit = new SimpleButton("save", form, "Save") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // get the indexer configuration and if valid
                IndexerDescriptor indexer = indexerConfigPanel.getIndexer();
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
                target.addComponent(ServicesConfigPanel.this);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    @Override
    public String getTitle() {
        return "Services Configuration";
    }
}