package org.artifactory.webapp.wicket.page.security.profile;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledActionPanel;
import org.artifactory.webapp.wicket.utils.validation.PasswordStreangthValidator;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ProfilePanel extends TitledActionPanel {

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AuthorizationService authService;

    public ProfilePanel(String id) {
        super(id);

        //Security sanity check
        boolean updatableProfile = authService.isUpdatableProfile();
        if (!updatableProfile) {
            throw new UnauthorizedInstantiationException(ProfilePanel.class);
        }

        final ProfileModel profile = new ProfileModel();
        UserInfo userInfo = userGroupService.currentUser();
        profile.setEmail(userInfo.getEmail());

        //Create the form
        Form form = new Form("userForm", new CompoundPropertyModel(profile));
        form.setOutputMarkupId(true);

        //Password
        PasswordTextField currentPassword = new PasswordTextField("currentPassword");
        form.add(currentPassword);
        PasswordTextField newPassword = new PasswordTextField("newPassword");
        form.add(newPassword.setRequired(false));
        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        form.add(retypedPassword.setRequired(false));
        form.add(new EqualPasswordInputValidator(newPassword, retypedPassword));

        //Email
        RequiredTextField emailTf = new RequiredTextField("email");
        emailTf.add(EmailAddressValidator.getInstance());
        form.add(emailTf);

        //Submit
        SimpleButton submit = new SimpleButton("submit", form, "Update") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                UserInfo userInfo = userGroupService.currentUser();
                String currentPasswordHashed = DigestUtils.md5Hex(profile.getCurrentPassword());
                if (!currentPasswordHashed.equals(userInfo.getPassword())) {
                    error("The specified current password is incorrect.");
                } else {
                    userInfo.setEmail(profile.getEmail());
                    if (StringUtils.hasText(profile.getNewPassword())) {
                        // Validate and update the new password only if entered by the user
                        List<IValidationError> errors = validatePassword(profile);
                        if (errors.size() != 0) {
                            error(errors.get(0));
                        } else {
                            userInfo.setPassword(DigestUtils.md5Hex(profile.getNewPassword()));
                        }
                    }
                }
                if (!this.hasErrorMessage()) {
                    userGroupService.updateUser(userInfo);
                    info("Profile successfully updated.");
                }
                form.clearInput();
                target.addComponent(form);
                FeedbackUtils.refreshFeedback(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        addDefaultButton(submit);

        //Cancel
        SimpleButton cancel = new SimpleButton("cancel", form, "Cancel") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                setResponsePage(Application.get().getHomePage());
            }
        };
        cancel.setDefaultFormProcessing(false);
        addButton(cancel);

        add(form);
    }

    private List<IValidationError> validatePassword(ProfileModel profile) {
        Validatable pass = new Validatable(profile.getNewPassword());
        PasswordStreangthValidator.getInstance().validate(pass);
        return pass.getErrors();
    }

    @Override
    public String getTitle() {
        return "User '" + authService.currentUsername() + "'";
    }
}
