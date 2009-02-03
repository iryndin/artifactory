/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.config.security;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpModel;
import org.artifactory.webapp.wicket.utils.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.utils.validation.UriValidator;
import org.artifactory.webapp.wicket.utils.validation.XsdNCNameValidator;
import org.springframework.util.StringUtils;

/**
 * Ldaps configuration panel.
 *
 * @author Yossi Shaul
 */
public class LdapCreateUpdatePanel extends CreateUpdatePanel<LdapSetting> {
    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private SecurityService securityService;

    SearchPattern searchPattern;

    @WicketProperty
    private String testUsername;

    @WicketProperty
    private String testPassword;

    public LdapCreateUpdatePanel(CreateUpdateAction action, LdapSetting ldapDescriptor,
            LdapsListPanel ldapsListPanel) {
        super(action, ldapDescriptor);
        setWidth(494);

        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        // Ldap key
        RequiredTextField ldapKeyField = new RequiredTextField("key");
        ldapKeyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            ldapKeyField.add(new XsdNCNameValidator("Invalid LDAP key '%s'"));
            ldapKeyField.add(new UniqueXmlIdValidator(getEditingDescriptor()));
        }
        border.add(ldapKeyField);
        border.add(new SchemaHelpBubble("key.help"));

        border.add(new StyledCheckbox("enabled"));

        TextField ldapUrlField = new RequiredTextField("ldapUrl");
        ldapUrlField.add(new UriValidator("ldap", "ldaps"));
        border.add(ldapUrlField);
        border.add(new SchemaHelpBubble("ldapUrl.help"));

        TitledBorder borderDn = new TitledBorder("borderDn");
        form.add(borderDn);
        borderDn.add(new TextField("userDnPattern"));
        borderDn.add(new SchemaHelpBubble("userDnPattern.help"));

        addSearchFields(ldapDescriptor, borderDn);

        addTestConnectionFields(ldapsListPanel);

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        SimpleButton submitButton = createSubmitButton(ldapsListPanel);
        form.add(submitButton);
        form.add(new DefaultButtonBehavior(submitButton));

        add(form);
    }

    private void addSearchFields(LdapSetting ldapDescriptor, TitledBorder borderDn) {
        searchPattern = ldapDescriptor.getSearch();
        if (searchPattern == null) {
            searchPattern = new SearchPattern();
        }

        borderDn.add(new TextField("searchFilter",
                new PropertyModel(searchPattern, "searchFilter")));
        borderDn.add(new SchemaHelpBubble("searchFilter.help",
                new SchemaHelpModel(searchPattern, "searchFilter")));

        borderDn.add(new TextField("searchBase", new PropertyModel(searchPattern, "searchBase")));
        borderDn.add(new SchemaHelpBubble("searchBase.help",
                new SchemaHelpModel(searchPattern, "searchBase")));

        borderDn.add(new StyledCheckbox(
                "searchSubTree", new PropertyModel(searchPattern, "searchSubTree")));

        borderDn.add(new TextField("managerDn", new PropertyModel(searchPattern, "managerDn")));
        borderDn.add(new SchemaHelpBubble("managerDn.help",
                new SchemaHelpModel(searchPattern, "managerDn")));

        PasswordTextField managerPasswordField = new PasswordTextField(
                "managerPassword", new PropertyModel(searchPattern, "managerPassword"));
        managerPasswordField.setRequired(false);
        managerPasswordField.setResetPassword(false);
        borderDn.add(managerPasswordField);
        borderDn.add(new SchemaHelpBubble("managerPassword.help",
                new SchemaHelpModel(searchPattern, "managerPassword")));
    }

    private void addTestConnectionFields(LdapsListPanel ldapsListPanel) {
        TitledBorder borderTest = new TitledBorder("borderTest");
        form.add(borderTest);
        borderTest.add(new TextField("testUsername", new PropertyModel(this, "testUsername")));
        borderTest.add(new HelpBubble(
                "testUsername.help", "Username to test the ldap connection"));

        PasswordTextField testPasswordField = new PasswordTextField(
                "testPassword", new PropertyModel(this, "testPassword"));
        testPasswordField.setRequired(false);
        testPasswordField.setResetPassword(false);
        borderTest.add(testPasswordField);
        borderTest.add(new HelpBubble(
                "testPassword.help", "Password to test the ldap connection"));

        // Test connection button
        borderTest.add(createTestConnectionButton(ldapsListPanel));
    }

    private SimpleButton createSubmitButton(final LdapsListPanel ldapsListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                LdapSetting ldapSetting = (LdapSetting) form.getModelObject();
                if (!validateAndUpdateLdapSettings(ldapSetting)) {
                    FeedbackUtils.refreshFeedback(target);
                    return;
                }

                if (isCreate()) {
                    getEditingDescriptor().getSecurity().addLdap(ldapSetting);
                    centralConfigService.saveEditedDescriptorAndReload();
                    info("Ldap '" + entity.getKey() + "' successfully created.");
                } else {
                    centralConfigService.saveEditedDescriptorAndReload();
                    info("Ldap '" + entity.getKey() + "' successfully updated.");
                }
                FeedbackUtils.refreshFeedback(target);
                target.addComponent(ldapsListPanel);
                ModalHandler.closeCurrent(target);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    private SimpleButton createTestConnectionButton(final LdapsListPanel ldapsListPanel) {
        SimpleButton submit = new SimpleButton("testLdap", form, "Test Connection") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                LdapSetting ldapSetting = (LdapSetting) form.getModelObject();
                if (!validateAndUpdateLdapSettings(ldapSetting)) {
                    return;
                }

                if (!StringUtils.hasText(testUsername) || !StringUtils.hasText(testUsername)) {
                    error("Please enter test username and password " +
                            "to test the LDAP settings");
                    return;
                }

                StatusHolder status = securityService
                        .testLdapConnection(ldapSetting, testUsername, testPassword);

                if (status.isError()) {
                    error(status.getStatusMsg());
                } else {
                    info(status.getStatusMsg());
                }

                FeedbackUtils.refreshFeedback(target);
            }


            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    private boolean validateAndUpdateLdapSettings(LdapSetting ldapSetting) {
        // validate userDnPattern or searchFilter
        boolean hasDnPattern = StringUtils.hasText(ldapSetting.getUserDnPattern());
        boolean hasSearch = StringUtils.hasText(searchPattern.getSearchFilter());
        if (!hasDnPattern && !hasSearch) {
            error("Ldap settings should provide a userDnPattern or a searchFilter (or both)");
            return false;
        }

        // if the search filter has value set the search pattern
        if (StringUtils.hasText(searchPattern.getSearchFilter())) {
            ldapSetting.setSearch(searchPattern);
        }

        return true;
    }


    protected MutableCentralConfigDescriptor getEditingDescriptor() {
        return centralConfigService.getDescriptorForEditing();
    }

    private LdapSetting getLdapSetting() {
        return entity;
    }

}