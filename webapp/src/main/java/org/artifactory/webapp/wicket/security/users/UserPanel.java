package org.artifactory.webapp.wicket.security.users;

import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.artifactory.security.ExtendedUserDetailsService;
import org.artifactory.security.SecurityHelper;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.CreateUpdatePanel;
import org.artifactory.webapp.wicket.components.DojoButton;
import org.artifactory.webapp.wicket.model.GPathPropertyModel;
import wicket.ajax.AjaxRequestTarget;
import wicket.extensions.ajax.markup.html.IndicatingAjaxSubmitButton;
import wicket.markup.html.form.CheckBox;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.form.validation.EqualPasswordInputValidator;
import wicket.model.CompoundPropertyModel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserPanel extends CreateUpdatePanel<User> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(UserPanel.class);

    private Form form;
    protected String header;

    public UserPanel(String id, final CreateUpdateAction action, final User user) {
        super(id, action, user);
        CompoundPropertyModel model = new GPathPropertyModel(entity);
        //Form
        form = new Form("userForm", model);
        form.setOutputMarkupId(true);
        final boolean create = action.equals(CreateUpdateAction.CREATE);
        header = create ? "New User" : "User Details";

        //Username
        RequiredTextField usernameTf = new RequiredTextField("username");
        form.add(usernameTf);
        usernameTf.setEnabled(create);
        //Password
        PasswordTextField password = new PasswordTextField("password");
        form.add(password);
        PasswordTextField retypedPassword = new PasswordTextField("retypedPassword");
        form.add(retypedPassword);
        form.add(new EqualPasswordInputValidator(password, retypedPassword));
        //Admin
        form.add(new CheckBox("admin"));
        //Cancel
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (create) {
                    form.clearInput();
                    clearFeedback(target);
                    target.addComponent(form);
                } else {
                    flip(target);
                }
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        String submitCaption = create ? "Create" : "Save";
        IndicatingAjaxSubmitButton submit = new DojoButton("submit", form, submitCaption) {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                ArtifactoryPage page = (ArtifactoryPage) getPage();
                SecurityHelper security = page.getContext().getSecurity();
                ExtendedUserDetailsService userDetailsService = security.getUserDetailsService();
                UserDetails details = entity.getUserDetails();
                if (create) {
                    boolean created = userDetailsService.createUser(details);
                    if (!created) {
                        error("User '" + entity.getUsername() + "' already exists.");
                    } else {
                        info("User '" + entity.getUsername() + "' successfully created.");
                        if (changeListener != null) {
                            target.addComponent(changeListener);
                        }
                        form.clearInput();
                        target.addComponent(form);
                    }
                    target.addComponent(feedback);
                } else {
                    userDetailsService.updateUser(details);
                    info("User '" + entity.getUsername() + "' successfully updated.");
                    flip(target);
                }
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);
        add(form);
    }

    @Override
    protected void onModelChanged() {
        //Notify the form
        CompoundPropertyModel model = new GPathPropertyModel(entity);
        form.setModel(model);
    }


    public String getTitle() {
        return header;
    }
}
