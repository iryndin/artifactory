package org.artifactory.webapp.wicket.security.profile;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ProfilePanel extends TitledPanel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ProfilePanel.class);

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
        form.add(newPassword);
        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        form.add(retypedPassword);
        form.add(new EqualPasswordInputValidator(newPassword, retypedPassword));

        //Email
        RequiredTextField emailTf = new RequiredTextField("email");
        emailTf.add(EmailAddressValidator.getInstance());
        form.add(emailTf);

        //Cancel
        SimpleButton cancel = new SimpleButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                setResponsePage(Application.get().getHomePage());
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        SimpleButton submit = new SimpleButton("submit", form, "Update") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                UserInfo userInfo = userGroupService.currentUser();
                String currentPasswordHashed = DigestUtils.md5Hex(profile.getCurrentPassword());
                if (currentPasswordHashed.equals(userInfo.getPassword())) {
                    userInfo.setPassword(DigestUtils.md5Hex(profile.getNewPassword()));
                    userInfo.setEmail(profile.getEmail());
                    userGroupService.updateUser(userInfo);
                    info("Profile successfully updated.");
                } else {
                    error("The specified current password is incorrect.");
                }
                form.clearInput();
                target.addComponent(form);
                FeedbackUtils.refreshFeedback(target);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        form.add(submit);
        add(form);
    }

    public String getTitle() {
        return "User Profile : " + authService.currentUsername();
    }
}
