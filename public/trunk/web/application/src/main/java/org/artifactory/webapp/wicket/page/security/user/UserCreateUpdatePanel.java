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

package org.artifactory.webapp.wicket.page.security.user;

import com.ocpsoft.pretty.time.PrettyTime;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.security.*;
import org.artifactory.api.util.Pair;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.deletable.listview.DeletableLabelGroup;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.component.panel.passwordstrength.PasswordStrengthComponentPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.PasswordStreangthValidator;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserCreateUpdatePanel extends CreateUpdatePanel<UserModel> {

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AddonsManager addons;

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private SecurityService securityService;

    PasswordTextField passwordField;
    PasswordTextField retypedPasswordField;

    StyledCheckbox adminCheckbox;
    StyledCheckbox updatableProfileCheckbox;

    public UserCreateUpdatePanel(CreateUpdateAction action, UserModel user, final UsersTable usersListTable) {
        super(action, user);
        setWidth(412);
        form.setOutputMarkupId(true);
        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        final boolean create = isCreate();

        //Username
        RequiredTextField usernameTf = new RequiredTextField("username");
        usernameTf.add(StringValidator.maximumLength(100));
        usernameTf.setEnabled(create);
        usernameTf.add(new JcrNameValidator("Invalid username '%s'"));
        border.add(usernameTf);

        //Password
        passwordField = new PasswordTextField("password");
        passwordField.setRequired(create);
        passwordField.add(PasswordStreangthValidator.getInstance());
        border.add(passwordField);

        final PasswordStrengthComponentPanel strength =
                new PasswordStrengthComponentPanel("strengthPanel", new PropertyModel(passwordField, "modelObject"));
        border.add(strength);
        strength.setOutputMarkupId(true);

        passwordField.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new NoAjaxIndicatorDecorator();
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                super.onError(target, e);
                String password = getFormComponent().getRawInput();
                passwordField.setModelObject(password);
                target.addComponent(strength);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(strength);
            }
        }.setThrottleDelay(Duration.seconds(0.5)));

        retypedPasswordField = new PasswordTextField("retypedPassword");
        retypedPasswordField.setRequired(create);
        border.add(retypedPasswordField);

        // validate password and retyped password
        form.add(new EqualPasswordInputValidator(passwordField, retypedPasswordField) {
            @Override
            public void validate(Form form) {
                if (entity.isDisableInternalPassword()) {
                    // no need to validate passwords if internal passwords are disabled
                    return;
                }
                if (!create && !StringUtils.hasText(passwordField.getModelObjectAsString())) {
                    return;
                }
                super.validate(form);
            }

            @Override
            protected String resourceKey() {
                return Classes.simpleName(EqualPasswordInputValidator.class);
            }
        });

        //Email
        RequiredTextField emailTf = new RequiredTextField("email");
        emailTf.add(EmailAddressValidator.getInstance());
        border.add(emailTf);

        //Admin
        adminCheckbox = new StyledCheckbox("admin");
        adminCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (adminCheckbox.isChecked()) {
                    updatableProfileCheckbox.setModelObject(Boolean.TRUE);
                }
                target.addComponent(updatableProfileCheckbox);
            }
        });
        adminCheckbox.setLabel(new Model("Admin"));
        adminCheckbox.setEnabled(addons.addonByType(CoreAddons.class).isNewAdminAccountAllowed());
        border.add(adminCheckbox);

        //Can update profile
        updatableProfileCheckbox = new StyledCheckbox("updatableProfile") {
            @Override
            public boolean isEnabled() {
                return !adminCheckbox.isChecked();
            }
        };
        updatableProfileCheckbox.setOutputMarkupId(true);
        border.add(updatableProfileCheckbox);

        // Internal password
        final StyledCheckbox disableInternalPassword = new StyledCheckbox("disableInternalPassword");
        if (create || entity.isAdmin()) {
            // disable if creating new user or it's an admin user
            disableInternalPassword.setEnabled(false);
        }

        disableInternalPassword.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (disableInternalPassword.isChecked()) {
                    disablePasswordFields();
                } else {
                    enablePasswordFields();
                }
                target.addComponent(form);
            }
        });
        border.add(disableInternalPassword);
        StringResourceModel helpMessage = new StringResourceModel("disableInternalPasswordHelp", this, null);
        border.add(new HelpBubble("disableInternalPasswordHelp", helpMessage));

        if (!create && user.isDisableInternalPassword()) {
            disablePasswordFields();
        }

        // groups
        Set<String> userGroups = user.getGroups();
        final DeletableLabelGroup<String> groupsListView = new DeletableLabelGroup<String>("groups", userGroups);
        groupsListView.setLabelClickable(false);
        groupsListView.setVisible(!create);
        border.add(groupsListView);
        String groupsLabelText = "Groups";
        if ((userGroups == null) || (userGroups.isEmpty())) {
            groupsLabelText = "User has no group memberships";
        }
        Label groupsLabel = new Label("groupsLabel", groupsLabelText);
        groupsLabel.setVisible(!create);
        border.add(groupsLabel);

        addLastLoginLabel(border);
        //addLastAccessLabel(border);

        //Cancel
        form.add(new ModalCloseLink("cancel"));

        //Submit
        String submitCaption = create ? "Create" : "Save";
        TitledAjaxSubmitLink submit = new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String username = entity.getUsername();
                boolean successful = true;
                if (create) {
                    successful = createNewUser(username);
                } else {
                    updateUser(username);
                }
                if (successful) {
                    usersListTable.refreshUsersList(target);
                    AjaxUtils.refreshFeedback(target);
                    ModalHandler.closeCurrent(target);
                }
            }

            private boolean createNewUser(String username) {
                UserInfoBuilder builder = new UserInfoBuilder(username);
                builder.password(DigestUtils.md5Hex(entity.getPassword()))
                        .email(entity.getEmail())
                        .admin(entity.isAdmin())
                        .updatableProfile(entity.isUpdatableProfile())
                        .groups(new HashSet<String>(groupsListView.getData()));
                UserInfo newUser = builder.build();

                boolean created = userGroupService.createUser(newUser);
                if (!created) {
                    error("User '" + username + "' already exists.");
                } else {
                    String successMessage = "User '" + username + "' successfully created.";
                    boolean userHasPermissions = authorizationService.userHasPermissions(username);
                    if (!userHasPermissions) {
                        successMessage += "\nUser has no assigned permissions yet. You can directly assign " +
                                "permissions to the user or add him to an exiting group that has assigned permissions.";
                    }
                    getPage().info(successMessage);
                }
                return created;
            }

            private void updateUser(String username) {
                // get the user info from the database and update it from the model
                UserInfo userInfo = userGroupService.findUser(username);
                userInfo.setEmail(entity.getEmail());
                userInfo.setAdmin(entity.isAdmin());
                userInfo.setUpdatableProfile(entity.isUpdatableProfile());
                userInfo.setGroups(new HashSet<String>(groupsListView.getData()));
                if (entity.isDisableInternalPassword()) {
                    // user should authentiate externally - set password to invalid
                    userInfo.setPassword(UserInfo.INVALID_PASSWORD);
                } else if (StringUtils.hasText(entity.getPassword())) {
                    userInfo.setPassword(DigestUtils.md5Hex(entity.getPassword()));
                }
                userGroupService.updateUser(userInfo);
                getPage().info("User '" + username + "' successfully updated.");
            }
        };
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private void enablePasswordFields() {
        passwordField.setEnabled(true);
        retypedPasswordField.setEnabled(true);
    }

    private void disablePasswordFields() {
        passwordField.setEnabled(false);
        retypedPasswordField.setEnabled(false);
    }

    private void addLastLoginLabel(TitledBorder border) {
        Pair<String, Long> lastLoginInfo = null;

        //If user exists
        if (!isCreate()) {
            lastLoginInfo = securityService.getUserLastLoginInfo(entity.getUsername());
        }
        final boolean loginInfoValid = (lastLoginInfo != null);

        Label lastLogin = new Label("lastLogin", new Model()) {
            @Override
            public boolean isVisible() {
                return loginInfoValid;
            }
        };
        border.add(lastLogin);
        if (loginInfoValid) {
            Date date = new Date(lastLoginInfo.getSecond());
            String clientIp = lastLoginInfo.getFirst();
            PrettyTime prettyTime = new PrettyTime();
            lastLogin.setModelObject("Last logged in: " + prettyTime.format(date) + " (" + date.toString() + "), from "
                    + clientIp + ".");
        }
    }

    private void addLastAccessLabel(TitledBorder border) {
        Pair<String, Long> lastAccessInfo = null;

        //If user exists
        if (!isCreate()) {
            lastAccessInfo = securityService.getUserLastAccessInfo(entity.getUsername());
        }
        final boolean loginAccessValid = (lastAccessInfo != null);

        Label lastAccess = new Label("lastAccess", new Model()) {
            @Override
            public boolean isVisible() {
                return loginAccessValid;
            }
        };
        border.add(lastAccess);
        if (loginAccessValid) {
            Date date = new Date(lastAccessInfo.getSecond());
            String clientIp = lastAccessInfo.getFirst();
            PrettyTime prettyTime = new PrettyTime();
            lastAccess.setModelObject("Last access in: " + prettyTime.format(date) + " (" + date.toString() + "), from "
                    + clientIp + ".");
        }
    }
}
