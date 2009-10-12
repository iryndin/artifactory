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

package org.artifactory.webapp.wicket.page.security.login;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WebApplicationAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.links.TitledPageLink;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.page.security.login.forgot.ForgotPasswordPage;

/**
 * @author Yoav Aharoni
 */
public class LoginPanel extends TitledActionPanel {

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private AddonsManager addons;


    @SpringBean
    private UserGroupService userGroupService;

    public LoginPanel(String string) {
        super(string);

        Label defaultCredentialsLabel = new Label("defaultCredentials", " (default: admin/password)");
        try {
            UserInfo userInfo = userGroupService.findUser("admin");
            boolean neverLoggedIn =
                    (userInfo.getLastLoginTimeMillis() == 0) && (StringUtils.isEmpty(userInfo.getLastLoginClientIp()));
            String password = userInfo.getPassword();
            boolean defaultPassword = (DigestUtils.md5Hex(SecurityService.DEFAULT_ADMIN_PASSWORD).equals(password));
            defaultCredentialsLabel.setVisible(neverLoggedIn && defaultPassword);
        } catch (Exception ignored) {
            defaultCredentialsLabel.setVisible(false);
        }
        add(defaultCredentialsLabel);

        //Add sign-in form to page, passing feedback panel as validation error handler
        add(new LoginForm("loginForm"));
    }

    /**
     * Sign in form.
     */
    public final class LoginForm extends Form {
        public LoginForm(String id) {
            super(id, new CompoundPropertyModel(new LoginInfo()));

            // add username
            TextField username = new TextField("username");
            username.setRequired(true);
            add(username);

            // add password
            PasswordTextField password = new PasswordTextField("password");
            password.setRequired(false);
            add(password);

            // add remember me checkbox
            add(new StyledCheckbox("rememberMe"));

            // add forgot password link
            if (isMailServerConfigured()) {
                addButton(new TitledPageLink("forgotPassword", "Forgot Password", ForgotPasswordPage.class));
            }

            // add login link
            TitledAjaxSubmitLink loginLink = addons.addonByType(WebApplicationAddon.class).getLoginLink("loginLink", this);
            addDefaultButton(loginLink);

            // add cancel link 
            addButton(new TitledPageLink("cancel", "Cancel", ArtifactoryApplication.get().getHomePage()));
        }

    }

    /**
     * Checks if the mail server is configured (central config descriptor and mail server descriptor are not null)
     *
     * @return boolean - True if mail server is configured.
     */
    private boolean isMailServerConfigured() {
        CentralConfigDescriptor descriptor = centralConfig.getDescriptor();
        return ((descriptor != null) && (descriptor.getMailServer() != null));
    }
}
