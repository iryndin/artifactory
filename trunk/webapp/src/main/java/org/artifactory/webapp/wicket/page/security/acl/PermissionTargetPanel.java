package org.artifactory.webapp.wicket.page.security.acl;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.webapp.wicket.common.component.CancelButton;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;

/**
 * Security configuration main form panel.
 *
 * @author Yossi Shaul
 */
class PermissionTargetPanel extends TitledActionPanel {

    @SpringBean
    private CentralConfigService centralConfigService;
    private StyledCheckbox annonAccess;

    public PermissionTargetPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        Form form = new Form("form");
        add(form);

        PropertyModel annonAccessModel = createAnnonymousAccessModel();
        annonAccess = new StyledCheckbox("anonAccessEnabled", annonAccessModel);

        annonAccess.setLabel(new Model("Allow Anonymous Access"));
        form.add(annonAccess);
        form.add(new PermissionTargetListPanel("permissionTargetList"));

        addDefaultButton(createSaveButton(form));
        addButton(new CancelButton(form));
    }

    private PropertyModel createAnnonymousAccessModel() {
        MutableCentralConfigDescriptor centralConfig =
                centralConfigService.getDescriptorForEditing();
        SecurityDescriptor securityDescriptor = centralConfig.getSecurity();
        PropertyModel annonAccessModel = new PropertyModel(securityDescriptor, "anonAccessEnabled");
        return annonAccessModel;
    }

    private SimpleButton createSaveButton(Form form) {
        SimpleButton submit = new SimpleButton("save", form, "Save") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                centralConfigService.saveEditedDescriptorAndReload();
                info("Security settings successfully updated.");
                FeedbackUtils.refreshFeedback(target);
                annonAccess.setModel(createAnnonymousAccessModel());
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
        return "";
    }
}
