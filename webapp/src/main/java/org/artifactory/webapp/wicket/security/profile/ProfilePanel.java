package org.artifactory.webapp.wicket.security.profile;

import org.apache.log4j.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.artifactory.security.ExtendedUserDetailsService;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.webapp.wicket.components.DojoButton;
import org.artifactory.webapp.wicket.panel.TitlePanel;
import org.artifactory.webapp.wicket.security.users.User;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ProfilePanel extends TitlePanel {
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
        AjaxButton submit = new DojoButton("submit", form, "Update") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);

                ArtifactoryContext context = ContextUtils.getContext();
                SecurityHelper security = context.getSecurity();
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
