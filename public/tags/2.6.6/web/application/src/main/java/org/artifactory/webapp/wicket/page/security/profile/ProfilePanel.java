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

package org.artifactory.webapp.wicket.page.security.profile;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.passwordstrength.PasswordStrengthComponentPanel;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.SetEnableVisitor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.CryptoHelper;
import org.artifactory.security.InternalUsernamePasswordAuthenticationToken;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.webapp.wicket.util.validation.PasswordStreangthValidator;
import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;

/**
 * @author Yoav Landman
 */
public class ProfilePanel extends TitledPanel {
    private static final Logger log = LoggerFactory.getLogger(ProfilePanel.class);
    private static final String HIDDEN_PASSWORD = "************";

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private SecurityService securityService;

    @SpringBean
    private AuthenticationManager authenticationManager;
    private Label encryptedPasswordLabel;
    private Form form;

    public ProfilePanel(String id, Form form, ProfileModel profile) {
        super(id);
        this.form = form;
        setOutputMarkupId(true);
        add(new CssClass("profile-panel"));
        setDefaultModel(new CompoundPropertyModel<ProfileModel>(profile));

        // current password
        final PasswordTextField currentPassword = new PasswordTextField("currentPassword");
        add(currentPassword);
        add(new HelpBubble("currentPassword.help", getString("currentPassword.help")));

        encryptedPasswordLabel = new Label("encryptedPassword", HIDDEN_PASSWORD);
        encryptedPasswordLabel.setVisible(securityService.isPasswordEncryptionEnabled());
        add(encryptedPasswordLabel);
        add(new HelpBubble("encryptedPassword.help", new ResourceModel("encryptedPassword.help")));

        // Profile update fields are only displayed for users with permissions to do so
        final WebMarkupContainer updateFieldsContainer = new WebMarkupContainer("updateFieldsContainer");
        updateFieldsContainer.setVisible(authService.isUpdatableProfile());
        add(updateFieldsContainer);

        addPasswordFields(updateFieldsContainer);

        // Email
        TextField<String> emailTf = new TextField<String>("email");
        emailTf.setEnabled(false);
        emailTf.add(EmailAddressValidator.getInstance());
        updateFieldsContainer.add(emailTf);

        // submit password
        add(new TitledAjaxSubmitLink("unlock", "Unlock") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                UserInfo userInfo = loadUserInfo();
                String enteredCurrentPassword = getUserProfile().getCurrentPassword();
                if (!authenticate(userInfo, enteredCurrentPassword)) {
                    error("The specified current password is incorrect.");
                } else {
                    unlockProfile(userInfo, target);
                }
                target.add(ProfilePanel.this);
                AjaxUtils.refreshFeedback(target);
            }

            private boolean authenticate(UserInfo userInfo, String enteredCurrentPassword) {
                try {
                    Authentication authentication = authenticationManager.authenticate(
                            new InternalUsernamePasswordAuthenticationToken(userInfo.getUsername(),
                                    enteredCurrentPassword));
                    return (authentication != null) && authentication.isAuthenticated();
                } catch (AuthenticationException e) {
                    return false;
                }
            }

            private void unlockProfile(UserInfo userInfo, AjaxRequestTarget target) {
                currentPassword.setEnabled(false);
                this.setEnabled(false);

                if (authService.isUpdatableProfile()) {
                    updateFieldsContainer.visitChildren(new SetEnableVisitor(true));
                }

                MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
                displayEncryptedPassword(mutableUser);

                send(getPage(), Broadcast.BREADTH, new ProfileEvent(target, mutableUser));
            }
        });
    }

    private void addPasswordFields(WebMarkupContainer updateFieldsContainer) {
        WebMarkupContainer passwordFieldsContainer = new WebMarkupContainer("passwordFieldsContainer");
        final TextField<String> newPassword;
        final WebMarkupContainer strength;
        final TextField<String> retypedPassword;

        if (authService.isDisableInternalPassword()) {
            newPassword = new TextField<String>("newPassword");
            strength = new WebMarkupContainer("strengthPanel");
            retypedPassword = new TextField<String>("retypedPassword");
            passwordFieldsContainer.setVisible(false);
        } else {
            // New password
            newPassword = new PasswordTextField("newPassword");
            newPassword.setRequired(false);
            newPassword.setEnabled(false);
            newPassword.add(PasswordStreangthValidator.getInstance());


            strength = new PasswordStrengthComponentPanel("strengthPanel",
                    new PropertyModel(newPassword, "modelObject"));
            strength.setOutputMarkupId(true);

            newPassword.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new NoAjaxIndicatorDecorator();
                }

                @Override
                protected void onError(AjaxRequestTarget target, RuntimeException e) {
                    super.onError(target, e);
                    String password = getFormComponent().getRawInput();
                    newPassword.setDefaultModelObject(password);
                    target.add(strength);
                }

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(strength);
                }
            }.setThrottleDelay(Duration.seconds(0.5)));

            retypedPassword = new PasswordTextField("retypedPassword");
            retypedPassword.setRequired(false);
            retypedPassword.setEnabled(false);
            form.add(new EqualPasswordInputValidator(newPassword, retypedPassword));
        }

        passwordFieldsContainer.add(newPassword);
        passwordFieldsContainer.add(strength);
        passwordFieldsContainer.add(retypedPassword);
        updateFieldsContainer.add(passwordFieldsContainer);
    }

    private ProfileModel getUserProfile() {
        return (ProfileModel) getDefaultModelObject();
    }

    private UserInfo loadUserInfo() {
        // load the user directly from the database. the instance returned from currentUser() might not
        // be with the latest changes
        return userGroupService.findUser(userGroupService.currentUser().getUsername());
    }

    @Override
    public String getTitle() {
        return "";
    }

    public void displayEncryptedPassword(MutableUserInfo mutableUser) {
        // generate a new KeyPair and update the user profile
        regenerateKeyPair(mutableUser);

        // display the encrypted password
        if (securityService.isPasswordEncryptionEnabled()) {
            String currentPassword = getUserProfile().getCurrentPassword();
            SecretKey secretKey = CryptoHelper.generatePbeKey(mutableUser.getPrivateKey());
            String encryptedPassword = CryptoHelper.encryptSymmetric(currentPassword, secretKey);
            encryptedPasswordLabel.setDefaultModelObject(encryptedPassword);
        }
    }

    private void regenerateKeyPair(MutableUserInfo mutableUser) {
        if (!StringUtils.hasText(mutableUser.getPrivateKey())) {
            log.debug("Generating new KeyPair for {}", mutableUser.getUsername());
            KeyPair keyPair = CryptoHelper.generateKeyPair();
            mutableUser.setPrivateKey(CryptoHelper.toBase64(keyPair.getPrivate()));
            mutableUser.setPublicKey(CryptoHelper.toBase64(keyPair.getPublic()));
            userGroupService.updateUser(mutableUser);
        }
    }
}
