package org.artifactory.webapp.wicket.page.security.profile;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.security.CryptoHelper;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.border.fieldset.FieldSetBorder;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxSubmitLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;
import org.artifactory.webapp.wicket.utils.validation.PasswordStreangthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
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
        PasswordTextField newPassword = new PasswordTextField("newPassword");
        newPassword.setRequired(false);
        newPassword.setEnabled(false);
        updateFieldsContainer.add(newPassword);

        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        retypedPassword.setRequired(false);
        retypedPassword.setEnabled(false);
        updateFieldsContainer.add(retypedPassword);

        profileForm.add(new EqualPasswordInputValidator(newPassword, retypedPassword));

        // Email
        TextField emailTf = new TextField("email");
        emailTf.setEnabled(false);
        emailTf.add(EmailAddressValidator.getInstance());
        updateFieldsContainer.add(emailTf);

        // Display settings.xml section with the encrypted password
        WebMarkupContainer settingsSnippet = new WebMarkupContainer(SETTINGS_SNIPPET_ID);
        settingsSnippet.setVisible(false);
        add(settingsSnippet);

        //Submit
        SimpleButton updateLink = createUpdateProfileButton(profileForm);
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
        return (ProfileModel) getModelObject();
    }

    private SimpleButton createUpdateProfileButton(final Form form) {
        return new SimpleButton("submit", form, "Update") {
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
                        // Validate and update the new password only if entered by the user
                        List<IValidationError> errors = validatePassword(profile);
                        if (errors.size() != 0) {
                            error(errors.get(0));
                        } else {
                            userInfo.setPassword(DigestUtils.md5Hex(newPassword));
                            profile.setCurrentPassword(newPassword);

                            // generate a new KeyPair and update the user profile
                            regenerateKeyPair(userInfo);

                            // display the encryptd password
                            if (securityService.isPasswordEncryptionEnabled()) {
                                displayEncryptedPassword(userInfo);
                            }
                        }
                    }
                }
                if (!this.hasErrorMessage()) {
                    userGroupService.updateUser(userInfo);
                    info("Profile successfully updated.");
                }
                form.clearInput();
                target.addComponent(ProfilePanel.this);
                FeedbackUtils.refreshFeedback(target);
            }
        };
    }

    private UserInfo loadUserInfo() {
        // load the user directly from the database. the instance returned from currentUser() might not
        // be with the latest changes
        return userGroupService.findUser(userGroupService.currentUser().getUsername());
    }

    @SuppressWarnings({"unchecked"})
    private List<IValidationError> validatePassword(ProfileModel profile) {
        Validatable pass = new Validatable(profile.getNewPassword());
        PasswordStreangthValidator.getInstance().validate(pass);
        return pass.getErrors();
    }

    @Override
    public String getTitle() {
        return "";
    }

    private void unlockProfile(UserInfo userInfo) {
        unlockForm.visitChildren(new IVisitor() {
            public Object component(Component component) {
                component.setEnabled(false);
                return CONTINUE_TRAVERSAL;
            }
        });

        profileForm.visitChildren(new IVisitor() {
            public Object component(Component component) {
                component.setEnabled(true);
                return CONTINUE_TRAVERSAL;
            }
        });

        getButtonsContainer().visitChildren(new IVisitor() {
            public Object component(Component component) {
                component.setEnabled(true);
                return CONTINUE_TRAVERSAL;
            }
        });

        // generate a new KeyPair and update the user profile
        regenerateKeyPair(userInfo);

        // display the encryptd password
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

        encryptedPasswordLabel.setModelObject(encryptedPassword);
        replace(settingsSnippet);
    }

    private Component createSettingXml(UserInfo userInfo, String encryptedPassword) {
        StringBuilder sb = new StringBuilder();
        sb.append("<server>\n");
        sb.append("    <id>${server-id}</id>\n");
        sb.append("    <username>").append(userInfo.getUsername()).append("</username>\n");
        sb.append("    <password>").append(encryptedPassword).append("</password>\n");
        sb.append("</server>");

        FieldSetBorder border = new FieldSetBorder("settingsBorder");
        add(border);

        border.add(new TextContentPanel("settingsDeclaration").setContent(sb.toString()));
        return border;
    }

    private class UnlockProfileButton extends TitledAjaxSubmitLink {
        public UnlockProfileButton(String id, String title) {
            super(id, title);
        }

        protected void onSubmit(AjaxRequestTarget target) {
            UserInfo userInfo = loadUserInfo();
            String password = getUserProfile().getCurrentPassword();
            if (!passwordValid(password, userInfo)) {
                error("The specified current password is incorrect.");
            } else {
                unlockProfile(userInfo);
            }
            target.addComponent(ProfilePanel.this);
            FeedbackUtils.refreshFeedback(target);
        }

        @Override
        protected void onError(AjaxRequestTarget target) {
            super.onError(target);
            FeedbackUtils.refreshFeedback(target);
        }

        private boolean passwordValid(String enteredCurrentPassword, UserInfo userInfo) {
            String currentPassword = userInfo.getPassword();
            if (!StringUtils.hasText(currentPassword)) {
                // ldap user - validate using the password in session
                return securityService.userPasswordMatches(enteredCurrentPassword);
            } else {
                // non-ldap user validate against hashed password in the database
                return currentPassword.equals(DigestUtils.md5Hex(enteredCurrentPassword));
            }
        }
    }
}
