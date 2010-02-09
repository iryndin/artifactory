package org.artifactory.webapp.wicket.security.profile;

import org.apache.log4j.Logger;
import org.artifactory.security.ExtendedUserDetailsService;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.DojoButton;
import org.artifactory.webapp.wicket.panel.WindowPanel;
import org.artifactory.webapp.wicket.security.users.User;
import wicket.Application;
import wicket.ajax.AjaxRequestTarget;
import wicket.ajax.markup.html.form.AjaxSubmitButton;
import wicket.authorization.UnauthorizedInstantiationException;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.form.validation.EqualPasswordInputValidator;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.CompoundPropertyModel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ProfilePanel extends WindowPanel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ProfilePanel.class);

    private FeedbackPanel feedback;
    private Profile profile;

    public ProfilePanel(String id) {
        super(id);

        //Security sanity check
        boolean updatableProfile = SecurityHelper.isUpdatableProfile();
        if (!updatableProfile) {
            throw new UnauthorizedInstantiationException(ProfilePanel.class);
        }

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

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
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                setResponsePage(Application.get().getHomePage());
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        AjaxSubmitButton submit = new DojoButton("submit", form, "Update") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);

                ArtifactoryPage page = (ArtifactoryPage) getPage();
                SecurityHelper security = page.getContext().getSecurity();
                ExtendedUserDetailsService userDetailsService = security.getUserDetailsService();

                SimpleUser simpleUser = SecurityHelper.getSimpleUser();
                //Create the web model
                User user = new User(simpleUser);

                if (profile.getCurrentPassword().equals(user.getPassword())) {
                    user.setPassword(profile.getNewPassword());
                    userDetailsService.updateUser(user.getUser());
                    info("Password successfully updated.");
                } else {
                    error("The current password specified is incorrect");
                }

                form.clearInput();
                target.addComponent(form);

                target.addComponent(feedback);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);
        add(form);
    }

    public String getTitle() {
        return "User Profile : " + SecurityHelper.getUsername();
    }
}
