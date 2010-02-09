/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.component.CancelLink;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.EncryptionPolicy;
import org.artifactory.descriptor.security.PasswordSettings;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpModel;

import java.util.Arrays;

/**
 * Security general configuration main panel.
 *
 * @author Yossi Shaul
 */
class SecurityGeneralConfigPanel extends TitledActionPanel {

    @SpringBean
    private CentralConfigService centralConfigService;

    private StyledCheckbox annonAccess;
    private DropDownChoice encryptionPoliciesDC;

    public SecurityGeneralConfigPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        Form form = new Form("form");
        add(form);

        MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
        SecurityDescriptor securityDescriptor = centralConfig.getSecurity();

        addAnnonymousAccessField(form, securityDescriptor);

        addEncryptionPolicyDropDown(form, securityDescriptor);

        addDefaultButton(createSaveButton(form));
        addButton(new CancelLink(form));
    }

    private void addEncryptionPolicyDropDown(Form form, SecurityDescriptor securityDescriptor) {
        PasswordSettings passwordSettings = securityDescriptor.getPasswordSettings();
        EncryptionPolicy[] encryptionPolicies = EncryptionPolicy.values();
        encryptionPoliciesDC = new DropDownChoice("encryptionPolicyType",
                new PropertyModel(passwordSettings, "encryptionPolicy"),
                Arrays.asList(encryptionPolicies));
        encryptionPoliciesDC.setChoiceRenderer(new EncryptionPolicyChoiceRenderer());
        form.add(encryptionPoliciesDC);
        form.add(new SchemaHelpBubble("encryptionPolicyType.help",
                new SchemaHelpModel(passwordSettings, "encryptionPolicy")));
    }

    private void addAnnonymousAccessField(Form form, SecurityDescriptor securityDescriptor) {
        annonAccess = new StyledCheckbox("anonAccessEnabled", new Model(securityDescriptor.isAnonAccessEnabled()));
        annonAccess.setLabel(new Model("Allow Anonymous Access"));
        form.add(annonAccess);
        form.add(new SchemaHelpBubble("anonAccessEnabled.help",
                new SchemaHelpModel(securityDescriptor, "anonAccessEnabled")));
    }

    private TitledAjaxSubmitLink createSaveButton(Form form) {
        return new TitledAjaxSubmitLink("save", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
                SecurityDescriptor securityDescriptor = centralConfig.getSecurity();
                securityDescriptor.setAnonAccessEnabled(annonAccess.isChecked());
                PasswordSettings passwordSettings = securityDescriptor.getPasswordSettings();
                passwordSettings.setEncryptionPolicy((EncryptionPolicy) encryptionPoliciesDC.getModelObject());
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

    private static class EncryptionPolicyChoiceRenderer extends ChoiceRenderer {
        @Override
        public Object getDisplayValue(Object object) {
            return WordUtils.capitalizeFully(object.toString());
        }
    }
}
