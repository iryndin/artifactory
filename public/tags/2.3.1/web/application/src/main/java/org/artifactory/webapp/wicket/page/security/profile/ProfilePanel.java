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

package org.artifactory.webapp.wicket.page.security.profile;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.passwordstrength.PasswordStrengthComponentPanel;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.SetEnableVisitor;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.CryptoHelper;
import org.artifactory.webapp.wicket.util.validation.PasswordStreangthValidator;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;

import static org.artifactory.common.wicket.component.label.highlighter.Syntax.xml;

/**
 * @author Yoav Landman
 */
public class ProfilePanel extends TitledActionPanel {
    private static final Logger log = LoggerFactory.getLogger(ProfilePanel.class);
    private static final String HIDDEN_PASSWORD = "************";
    private static final String UNLOCK_FORM_ID = "unlockForm";
    private static final String USER_FORM_ID = "userForm";
    private static final String SETTINGS_SNIPPET_ID = "settingsSnippet";

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private SecurityService securityService;

    @SpringBean
    private CentralConfigService centralConfigService;

    private Label encryptedPasswordLabel;
    private Form unlockForm;
    private Form profileForm;

    public ProfilePanel(String id) {
        super(id, new CompoundPropertyModel(new ProfileModel()));
        setOutputMarkupId(true);
        add(new CssClass("profile-panel"));


        // load user email
        ProfileModel profile = getUserProfile();
        profile.setEmail(loadUserInfo().getEmail());

        // unlock form
        unlockForm = new Form(UNLOCK_FORM_ID);
        add(unlockForm);

        // current password
        final PasswordTextField currentPassword = new PasswordTextField("currentPassword");
        unlockForm.add(currentPassword);
        unlockForm.add(new HelpBubble("currentPassword.help", getString("currentPassword.help", null)));

        // submit password
        final TitledAjaxSubmitLink submitPassword = new UnlockProfileButton("unlock", "Unlock");
        unlockForm.setDefaultButton(submitPassword);
        unlockForm.add(submitPassword);

        // user profile form
        profileForm = new Form(USER_FORM_ID);
        profileForm.setOutputMarkupId(true);
        add(profileForm);

        encryptedPasswordLabel = new Label("encryptedPassword", HIDDEN_PASSWORD);
        encryptedPasswordLabel.setVisible(securityService.isPasswordEncryptionEnabled());
        profileForm.add(encryptedPasswordLabel);

        profileForm.add(new HelpBubble("encryptedPassword.help", new ResourceModel("encryptedPassword.help")));

        // Profile update fields are only displayed for users with permissions to do so
        WebMarkupContainer updateFieldsContainer = new WebMarkupContainer("updateFieldsContainer");
        updateFieldsContainer.setVisible(authService.isUpdatableProfile());
        profileForm.add(updateFieldsContainer);

        // New password
        final PasswordTextField newPassword = new PasswordTextField("newPassword");
        newPassword.setRequired(false);
        newPassword.setEnabled(false);
        newPassword.add(PasswordStreangthValidator.getInstance());
        updateFieldsContainer.add(newPassword);

        final PasswordStrengthComponentPanel strength =
                new PasswordStrengthComponentPanel("strengthPanel", new PropertyModel(newPassword, "modelObject"));
        updateFieldsContainer.add(strength);
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
                target.addComponent(strength);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(strength);
            }
        }.setThrottleDelay(Duration.seconds(0.5)));

        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        retypedPassword.setRequired(false);
        retypedPassword.setEnabled(false);
        updateFieldsContainer.add(retypedPassword);

        profileForm.add(new EqualPasswordInputValidator(newPassword, retypedPassword));

        // Email
        TextField<String> emailTf = new TextField<String>("email");
        emailTf.setEnabled(false);
        emailTf.add(EmailAddressValidator.getInstance());
        updateFieldsContainer.add(emailTf);

        // Display settings.xml section with the encrypted password
        WebMarkupContainer settingsSnippet = new WebMarkupContainer(SETTINGS_SNIPPET_ID);
        settingsSnippet.setVisible(false);
        add(settingsSnippet);

        //Submit
        TitledAjaxSubmitLink updateLink = createUpdateProfileButton(profileForm);
        updateLink.setEnabled(false);
        addDefaultButton(updateLink);

        //Cancel
        TitledAjaxLink cancelLink = new TitledAjaxLink("cancel", "Cancel") {
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(Application.get().getHomePage());
            }
        };
        cancelLink.setEnabled(false);

        addButton(cancelLink);

    }

    private ProfileModel getUserProfile() {
        return (ProfileModel) getDefaultModelObject();
    }

    private TitledAjaxSubmitLink createUpdateProfileButton(final Form form) {
        return new TitledAjaxSubmitLink("submit", "Update", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ProfileModel profile = getUserProfile();
                UserInfo userInfo = loadUserInfo();
                String currentPasswordHashed = DigestUtils.md5Hex(profile.getCurrentPassword());
                if (!currentPasswordHashed.equals(userInfo.getPassword())) {
                    error("The specified current password is incorrect.");
                } else if (!StringUtils.hasText(profile.getEmail())) {
                    error("Field 'Email address' is required.");
                } else {
                    userInfo.setEmail(profile.getEmail());
                    String newPassword = profile.getNewPassword();
                    if (StringUtils.hasText(newPassword)) {
                        userInfo.setPassword(DigestUtils.md5Hex(newPassword));
                        profile.setCurrentPassword(newPassword);

                        // generate a new KeyPair and update the user profile
                        regenerateKeyPair(userInfo);

                        // display the encrypted password
                        if (securityService.isPasswordEncryptionEnabled()) {
                            displayEncryptedPassword(userInfo);
                        }
                    }
                }
                if (!this.hasErrorMessage()) {
                    userGroupService.updateUser(userInfo);
                    AccessLogger.updated("User " + userInfo.getUsername() + " has updated his profile successfully");
                    info("Profile successfully updated.");
                }
                form.clearInput();
                target.addComponent(ProfilePanel.this);
                AjaxUtils.refreshFeedback(target);
            }
        };
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

    private void unlockProfile(UserInfo userInfo) {
        unlockForm.visitChildren(new SetEnableVisitor<Component>(false));

        profileForm.visitChildren(new SetEnableVisitor<Component>(true));

        getButtonsContainer().visitChildren(new SetEnableVisitor<Component>(true));

        // generate a new KeyPair and update the user profile
        regenerateKeyPair(userInfo);

        // display the encrypted password
        if (securityService.isPasswordEncryptionEnabled()) {
            displayEncryptedPassword(userInfo);
        }
    }

    private void regenerateKeyPair(UserInfo userInfo) {
        if (!StringUtils.hasText(userInfo.getPrivateKey())) {
            log.debug("Generating new KeyPair for {}", userInfo.getUsername());
            KeyPair keyPair = CryptoHelper.generateKeyPair();
            userInfo.setPrivateKey(CryptoHelper.toBase64(keyPair.getPrivate()));
            userInfo.setPublicKey(CryptoHelper.toBase64(keyPair.getPublic()));
            userGroupService.updateUser(userInfo);
        }
    }

    private void displayEncryptedPassword(UserInfo userInfo) {
        WebMarkupContainer settingsSnippet = new WebMarkupContainer(SETTINGS_SNIPPET_ID);
        String currentPassword = getUserProfile().getCurrentPassword();
        SecretKey secretKey = CryptoHelper.generatePbeKey(userInfo.getPrivateKey());
        String encryptedPassword = CryptoHelper.encryptSymmetric(currentPassword, secretKey);
        settingsSnippet.add(createSettingXml(userInfo, encryptedPassword));
        settingsSnippet.add(new Label("nonMavenPassword",
                "Non-maven clients should use a non-escaped password: " + encryptedPassword));
        encryptedPasswordLabel.setDefaultModelObject(encryptedPassword);
        replace(settingsSnippet);
    }

    private Component createSettingXml(UserInfo userInfo, String encryptedPassword) {
        String escapedEncPassword = CryptoHelper.escapeEncryptedPassword(encryptedPassword);
        StringBuilder sb = new StringBuilder();
        sb.append("<server>\n");
        sb.append("    <id>${server-id}</id>\n");
        sb.append("    <username>").append(userInfo.getUsername()).append("</username>\n");
        sb.append("    <password>").append(escapedEncPassword).append("</password>\n");
        sb.append("</server>");

        FieldSetBorder border = new FieldSetBorder("settingsBorder");
        add(border);

        border.add(WicketUtils.getSyntaxHighlighter("settingsDeclaration", sb.toString(), xml));
        return border;
    }

    private class UnlockProfileButton extends TitledAjaxSubmitLink {
        public UnlockProfileButton(String id, String title) {
            super(id, title);
        }

        @Override
        protected void onSubmit(AjaxRequestTarget target, Form form) {
            UserInfo userInfo = loadUserInfo();
            String password = getUserProfile().getCurrentPassword();
            if (!passwordValid(password, userInfo)) {
                error("The specified current password is incorrect.");
            } else {
                unlockProfile(userInfo);
            }
            target.addComponent(ProfilePanel.this);
            AjaxUtils.refreshFeedback(target);
        }

        private boolean passwordValid(String enteredCurrentPassword, UserInfo userInfo) {
            String currentPassword = userInfo.getPassword();
            if (!StringUtils.hasText(currentPassword)) {
                // external user - validate using the password in session
                return securityService.userPasswordMatches(enteredCurrentPassword);
            } else {
                // internal user validate against hashed password in the database
                return currentPassword.equals(DigestUtils.md5Hex(enteredCurrentPassword));
            }
        }
    }
}
