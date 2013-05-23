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

package org.artifactory.webapp.wicket.page.config.license;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.page.base.BasePage;

import java.io.IOException;

/**
 * Artifactory license key management panel.
 *
 * @author Yossi Shaul
 */
public class LicensePanel extends TitledPanel {

    @SpringBean
    private CentralConfigService configService;

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private AuthorizationService authService;

    @WicketProperty
    private String licenseKey;

    public LicensePanel(String id) {
        super(id);
        add(new CssClass("general-settings-panel"));

        add(new Label("licenseKeyLabel", "License Key"));
        add(new HelpBubble("licenseKey.help",
                "A license key that uniquely validates this Artifactory server instance.\n" +
                        "The license key is required for using Artifactory Add-ons."));

        licenseKey = addonsManager.getLicenseKey();
        TextArea licenseKeyTextField = new TextArea("licenseKey", new PropertyModel(this, "licenseKey"));
        licenseKeyTextField.add(new LicenseKeyValidator());
        add(licenseKeyTextField);

        FieldSetBorder licenseDetails = new FieldSetBorder("licenseDetails");
        add(licenseDetails);
        if (!addonsManager.isLicenseInstalled()) {
            licenseDetails.setVisible(false);
        } else {
            String[] details = addonsManager.getLicenseDetails();
            licenseDetails.add(new Label("subject", details[0]));
            licenseDetails.add(new Label("expiry", details[1]));
            licenseDetails.add(new Label("type", details[2]));
        }
    }

    TitledAjaxSubmitLink createSaveButton(Form form) {
        TitledAjaxSubmitLink saveButton = new TitledAjaxSubmitLink("save", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    addonsManager.installLicense(licenseKey);
                    // rebuild the site map and refresh the whole page to reload the new site map
                    ArtifactoryApplication.get().rebuildSiteMap();
                    Session.get().info("Successfully installed license.");
                    Class<? extends BasePage> redirectPage =
                            authService.isAdmin() ? LicensePage.class : ArtifactoryApplication.get().getHomePage();
                    setResponsePage(redirectPage);
                } catch (IOException e) {
                    error("Failed to install license");
                    AjaxUtils.refreshFeedback(target);
                }
            }
        };
        form.add(new DefaultButtonBehavior(saveButton));
        return saveButton;
    }
}