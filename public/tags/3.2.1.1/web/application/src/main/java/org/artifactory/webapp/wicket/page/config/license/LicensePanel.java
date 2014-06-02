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
import org.artifactory.addon.ArtifactoryRunningMode;
import org.artifactory.addon.license.VerificationResult;
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
import org.artifactory.state.ArtifactoryServerState;
import org.artifactory.state.model.ArtifactoryStateManager;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.page.base.BasePage;

import static org.artifactory.addon.license.VerificationResult.*;

/**
 * Artifactory license key management panel.
 *
 * @author Yossi Shaul
 */
public class LicensePanel extends TitledPanel {
    private final String NOT_SUPPORT_FOR_SWITCHING_MODE_ON_RUNTIME = "Changing Artifactory mode to offline" +
            " since Artifactory doesn't allow to switch its mode during run time. Please restart the server";
    private final String SUCCESSFULLY_INSTALL = "The license has been successfully installed.";

    @SpringBean
    private CentralConfigService configService;

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private ArtifactoryStateManager stateManager;

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
                ArtifactoryRunningMode oldRunningMode = addonsManager.getArtifactoryRunningMode();
                VerificationResult result = addonsManager.installLicense(licenseKey);
                ArtifactoryRunningMode newRunningMode = addonsManager.getArtifactoryRunningMode();
                boolean sameMode = ArtifactoryRunningMode.sameMode(oldRunningMode, newRunningMode);
                if (result == valid) {
                    if (sameMode) {
                        handleSuccessFullInstall();
                    } else {
                        String message = SUCCESSFULLY_INSTALL + " " + NOT_SUPPORT_FOR_SWITCHING_MODE_ON_RUNTIME;
                        handleSwitchingToOffline(message, target);
                    }
                } else if (error == result || converting == result || invalidKey == result) {
                    handleError(target);
                } else {
                    String message = SUCCESSFULLY_INSTALL + result.showMassage();
                    handleSwitchingToOffline(message, target);
                }
            }

            private void handleSuccessFullInstall() {
                boolean success = stateManager.forceState(ArtifactoryServerState.RUNNING);
                rebuildSiteMap();
                if (success) {
                    Session.get().info(SUCCESSFULLY_INSTALL);
                } else {
                    Session.get().warn(
                            SUCCESSFULLY_INSTALL + " In order for the change will take place, please restart the server");
                }
            }

            private void handleError(AjaxRequestTarget target) {
                error("Failed to install license. An error occurred during the license installation.");
                AjaxUtils.refreshFeedback(target);
            }

            private void handleSwitchingToOffline(String message, AjaxRequestTarget target) {
                stateManager.forceState(ArtifactoryServerState.OFFLINE);
                rebuildSiteMap();
                Session.get().warn(message);
                AjaxUtils.refreshFeedback(target);
            }


            private void rebuildSiteMap() {
                // rebuild the site map and refresh the whole page to reload the new site map
                ArtifactoryApplication.get().rebuildSiteMap();
                Class<? extends BasePage> redirectPage = authService.isAdmin() ? LicensePage.class :
                        ArtifactoryApplication.get().getHomePage();

                setResponsePage(redirectPage);
            }
        };
        form.add(new DefaultButtonBehavior(saveButton));
        return saveButton;
    }
}