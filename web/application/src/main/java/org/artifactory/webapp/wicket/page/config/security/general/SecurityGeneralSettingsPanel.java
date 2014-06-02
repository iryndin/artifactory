/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config.security.general;

import org.apache.commons.lang.WordUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.component.CancelLink;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.EncryptionPolicy;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

import java.util.Arrays;

/**
 * Security general configuration main panel.
 *
 * @author Yossi Shaul
 */
class SecurityGeneralConfigPanel extends TitledActionPanel {

    @SpringBean
    private CentralConfigService centralConfigService;

    public SecurityGeneralConfigPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
        SecurityDescriptor securityDescriptor = centralConfig.getSecurity();
        CompoundPropertyModel<SecurityDescriptor> securityModel =
                new CompoundPropertyModel<>(securityDescriptor);
        Form<SecurityDescriptor> form = new Form<>("form", securityModel);
        add(form);

        addAnonymousAccessField(form);

        addHideUnauthorizedResourcesField(form);

        addEncryptionPolicyDropDown(form);

        addDefaultButton(createSaveButton(form));
        addButton(new CancelLink(form));
    }

    private void addAnonymousAccessField(Form form) {
        StyledCheckbox anonAccess = new StyledCheckbox("anonAccessEnabled");
        anonAccess.setLabel(Model.of("Allow Anonymous Access"));
        form.add(anonAccess);
        form.add(new SchemaHelpBubble("anonAccessEnabled.help"));
    }

    private void addHideUnauthorizedResourcesField(Form form) {
        StyledCheckbox anonAccess = new StyledCheckbox("hideUnauthorizedResources");
        anonAccess.setLabel(Model.of("Hide Existence of Unauthorized Resources"));
        form.add(anonAccess);
        form.add(new SchemaHelpBubble("hideUnauthorizedResources.help"));
    }

    private void addEncryptionPolicyDropDown(Form form) {
        EncryptionPolicy[] encryptionPolicies = EncryptionPolicy.values();
        DropDownChoice<EncryptionPolicy> encryptionPoliciesDC = new DropDownChoice<>(
                "passwordSettings.encryptionPolicy",
                Arrays.asList(encryptionPolicies));
        encryptionPoliciesDC.setChoiceRenderer(new EncryptionPolicyChoiceRenderer());
        form.add(encryptionPoliciesDC);
        form.add(new SchemaHelpBubble("passwordSettings.encryptionPolicy.help", "passwordSettings.encryptionPolicy"));
    }

    private TitledAjaxSubmitLink createSaveButton(Form<SecurityDescriptor> form) {
        return new TitledAjaxSubmitLink("save", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
                SecurityDescriptor editedDescriptor = (SecurityDescriptor) form.getModelObject();
                centralConfig.setSecurity(editedDescriptor);
                centralConfigService.saveEditedDescriptorAndReload(centralConfig);
                info("Security settings successfully updated.");
                AjaxUtils.refreshFeedback(target);
            }
        };
    }

    @Override
    public String getTitle() {
        return "";
    }

    private static class EncryptionPolicyChoiceRenderer extends ChoiceRenderer<EncryptionPolicy> {
        @Override
        public String getDisplayValue(EncryptionPolicy policy) {
            return WordUtils.capitalizeFully(policy.toString());
        }
    }
}
