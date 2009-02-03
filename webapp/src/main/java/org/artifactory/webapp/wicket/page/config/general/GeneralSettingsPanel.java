package org.artifactory.webapp.wicket.page.config.general;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.webapp.wicket.common.behavior.AjaxCallConfirmationDecorator;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General settings (server name, max upload, etc.) configuration panel.
 *
 * @author Yossi Shaul
 */
public class GeneralSettingsPanel extends TitledActionPanel {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralSettingsPanel.class);

    @SpringBean
    private CentralConfigService centralConfigService;

    public GeneralSettingsPanel(String id, Form form) {
        super(id);
        setOutputMarkupId(true);
        add(new CssClass("general-settings-panel"));
        initPanelModel();

        add(new TextField("serverName"));
        add(new RequiredTextField("fileUploadMaxSizeMb", Integer.class));
        add(new RequiredTextField("dateFormat"));
        add(new StyledCheckbox("offlineMode"));

        add(new SchemaHelpBubble("serverName.help"));
        add(new SchemaHelpBubble("fileUploadMaxSizeMb.help"));
        add(new SchemaHelpBubble("dateFormat.help"));
        add(new SchemaHelpBubble("offlineMode.help"));

        add(createReloadButton());

        addDefaultButton(createSaveButton(form));
        addButton(createCancelButton());
    }

    private void initPanelModel() {
        setModel(new CompoundPropertyModel(centralConfigService.getDescriptorForEditing()));
    }

    private TitledAjaxLink createReloadButton() {
        return new TitledAjaxLink("reload", "Reload Configuration from XML") {
            public void onClick(AjaxRequestTarget target) {
                try {
                    centralConfigService.reload();
                    info("Configuration reloaded successfully.");
                } catch (Exception e) {
                    String msg = "Failed to reload configuration: " + e.getMessage();
                    error(msg);
                    LOG.error(msg, e);
                }
                initPanelModel();
                // refresh all the panel to reload new descriptor
                target.addComponent(GeneralSettingsPanel.this);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxCallConfirmationDecorator(super.getAjaxCallDecorator(),
                        "Are you sure you want to reolad from the configuration file?");
            }
        };
    }

    private TitledAjaxLink createCancelButton() {
        return new TitledAjaxLink("cancel", "Cancel") {
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(GeneralConfigPage.class);
            }
        };
    }

    private SimpleButton createSaveButton(Form form) {
        return new SimpleButton("save", form, "Save") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                centralConfigService.saveEditedDescriptorAndReload();
                info("General settings successfully updated.");
                FeedbackUtils.refreshFeedback(target);
                // reload the central config
                initPanelModel();
                target.addComponent(GeneralSettingsPanel.this);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
    }

    @Override
    public String getTitle() {
        return "General Settings";
    }

}