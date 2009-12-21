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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WebApplicationAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.components.container.LogoLinkContainer;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.config.general.addon.AddonSettingsPanel;
import org.slf4j.Logger;

import java.io.File;

/**
 * Security configuration page.
 *
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class GeneralConfigPage extends AuthenticatedPage {

    private static final Logger log = LoggerFactory.getLogger(GeneralConfigPage.class);


    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private CentralConfigService centralConfigService;

    private CompoundPropertyModel configDescriptorModel;
    private CustomizingPanel lookAndFeelPanel;

    public GeneralConfigPage() {
        setOutputMarkupId(true);
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        configDescriptorModel = new CompoundPropertyModel(mutableDescriptor);
        Form form = new Form("form", configDescriptorModel);
        add(form);
        WebApplicationAddon applicationAddon = addonsManager.addonByType(WebApplicationAddon.class);
        form.add(new GeneralSettingsPanel("generalConfigPanel"));
        lookAndFeelPanel = applicationAddon.getCustomizingPanel("customizingPanel");
        form.add(lookAndFeelPanel);
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
                FileUtils.deleteQuietly(new File(lookAndFeelPanel.getLogoPath()));
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
                String logo = getLogo(centralConfig);
                if (!StringUtils.isBlank(logo)) {
                    try {
                        String logoPath = centralConfigService.updateLogo(logo, lookAndFeelPanel.isDeleteFile());
                        centralConfig.setLogo(logoPath);
                    } catch (Exception e) {
                        String errorMessage = "Could not upload logo";
                        log.error(errorMessage, e);
                        error(errorMessage);
                    }
                }
                //Override cookie value if the admin decides to enforce the addon info state
                if (!existingAddonSettings.isShowAddonsInfo() && newAddonSettings.isShowAddonsInfo()) {
                    newAddonSettings.setShowAddonsInfoCookie(Long.toString(System.currentTimeMillis()));
                }
                centralConfigService.saveEditedDescriptorAndReload(centralConfig);
                serverIdChanged(currentServerId, newServerId);
                updateModel();
                LogoLinkContainer logoLink = new LogoLinkContainer("artifactoryLink");
                MarkupContainer logoContainer =
                        ((WebMarkupContainer) getPage().get("logoWrapper")).addOrReplace(logoLink);
                target.addComponent(logoContainer);
                Label footer = new Label("footer", new Model(centralConfig.getFooter()));
                MarkupContainer footerContainer =
                        ((WebMarkupContainer) getPage().get("footerWrapper")).addOrReplace(footer);
                target.addComponent(footerContainer);
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

    private String getLogo(CentralConfigDescriptorImpl centralConfig) {
        if (!StringUtils.isBlank(centralConfig.getLogo())) {
            return centralConfig.getLogo();
        } else if (!StringUtils.isBlank(lookAndFeelPanel.getLogoPath())) {
            return lookAndFeelPanel.getLogoPath();
        } else {
            return "";
        }
    }

    private void serverIdChanged(String currentServerId, String newServerId) {
        addonsManager.onServerIdUpdated(currentServerId, newServerId);
    }

    private CentralConfigDescriptorImpl getCentralConfigDescriptorFromModel() {
        return (CentralConfigDescriptorImpl) configDescriptorModel.getObject();
    }
}