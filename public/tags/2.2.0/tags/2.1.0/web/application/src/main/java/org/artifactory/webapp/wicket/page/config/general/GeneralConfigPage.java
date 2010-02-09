/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.config.general;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.config.general.addon.AddonSettingsPanel;

/**
 * Security configuration page.
 *
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class GeneralConfigPage extends AuthenticatedPage {

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private CentralConfigService centralConfigService;

    private CompoundPropertyModel configDescriptorModel;

    public GeneralConfigPage() {
        setOutputMarkupId(true);
        configDescriptorModel = new CompoundPropertyModel(centralConfigService.getMutableDescriptor());
        Form form = new Form("form", configDescriptorModel);
        add(form);

        form.add(new GeneralSettingsPanel("generalConfigPanel"));
        form.add(new AddonSettingsPanel("addonsSettingsPanel"));

        add(createSaveButton(form));
        add(createCancelButton());
    }

    private void updateModel() {
        MutableCentralConfigDescriptor configDescriptor = centralConfigService.getMutableDescriptor();
        configDescriptorModel.setObject(configDescriptor);
    }

    private TitledAjaxLink createCancelButton() {
        return new TitledAjaxLink("cancel", "Cancel") {
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(GeneralConfigPage.class);
            }
        };
    }

    private TitledAjaxSubmitLink createSaveButton(Form form) {
        TitledAjaxSubmitLink saveButton = new TitledAjaxSubmitLink("save", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                CentralConfigDescriptorImpl centralConfig = getCentralConfigDescriptorFromModel();
                AddonSettings existingAddonSettings = centralConfigService.getDescriptor().getAddons();
                AddonSettings newAddonSettings = centralConfig.getAddons();
                String newServerId = newAddonSettings.getServerId();
                String currentServerId = existingAddonSettings.getServerId();

                //Override cookie value if the admin decides to enforce the addon info state
                if (!existingAddonSettings.isShowAddonsInfo() && newAddonSettings.isShowAddonsInfo()) {
                    newAddonSettings.setShowAddonsInfoCookie(Long.toString(System.currentTimeMillis()));
                }
                centralConfigService.saveEditedDescriptorAndReload(centralConfig);
                serverIdChanged(currentServerId, newServerId);
                updateModel();
                info("Settings successfully updated.");
                AjaxUtils.refreshFeedback(target);
            }
        };
        form.add(new DefaultButtonBehavior(saveButton));
        return saveButton;
    }

    @Override
    public String getPageName() {
        return "General Configuration";
    }

    private void serverIdChanged(String currentServerId, String newServerId) {
        addonsManager.onServerIdUpdated(currentServerId, newServerId);
    }

    private CentralConfigDescriptorImpl getCentralConfigDescriptorFromModel() {
        return (CentralConfigDescriptorImpl) configDescriptorModel.getObject();
    }
}