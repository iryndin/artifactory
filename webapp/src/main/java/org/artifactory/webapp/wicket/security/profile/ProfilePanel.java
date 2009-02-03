package org.artifactory.webapp.wicket.security.profile;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.security.JcrUserDetailsService;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.SimpleButton;
import org.artifactory.webapp.wicket.components.panel.FeedbackEnabledPanel;
import org.artifactory.webapp.wicket.security.users.User;
import org.springframework.security.userdetails.UserDetails;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ProfilePanel extends FeedbackEnabledPanel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ProfilePanel.class);

    private Profile profile;

    public ProfilePanel(String id) {
        super(id);

        //Security sanity check
        boolean updatableProfile = ArtifactorySecurityManager.isUpdatableProfile();
        if (!updatableProfile) {
            throw new UnauthorizedInstantiationException(ProfilePanel.class);
        }

        this.profile = new Profile();
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
                ArtifactoryContext context = ContextHelper.get();
                ArtifactorySecurityManager security = context.getSecurity();
                JcrUserDetailsService userDetailsService = security.getUserDetailsService();

                String currentUserName = ArtifactorySecurityManager.getUsername();
                UserDetails currentUser =
                        security.getUserDetailsService().loadUserByUsername(currentUserName);
                SimpleUser simpleUser = new SimpleUser(currentUser);
                //Create the web model
                User user = new User(simpleUser);
                String hashedPassword = DigestUtils.md5Hex(profile.getCurrentPassword());
                if (hashedPassword.equals(simpleUser.getPassword())) {
                    user.setPassword(profile.getNewPassword());
                    SimpleUser updatedSimpleUser = user.getUser();
                    userDetailsService.updateUser(updatedSimpleUser);
                    info("Password successfully updated.");
                } else {
                    error("The specified current password is incorrect.");
                }
                form.clearInput();
                target.addComponent(form);
                target.addComponent(getFeedback());
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(getFeedback());
            }
        };
        form.add(submit);
        add(form);
    }

    public String getTitle() {
        return "User Profile : " + ArtifactorySecurityManager.getUsername();
    }
}
