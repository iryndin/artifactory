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
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpModel;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.UriValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;
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

    public LdapCreateUpdatePanel(CreateUpdateAction action, LdapSetting ldapDescriptor, LdapsListPanel ldapsListPanel) {
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
            ldapKeyField.add(new UniqueXmlIdValidator(ldapsListPanel.getMutableDescriptor()));
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
        borderDn.add(new StyledCheckbox("autoCreateUser"));
        borderDn.add(new SchemaHelpBubble("userDnPattern.help"));

        addSearchFields(ldapDescriptor, borderDn);

        addTestConnectionFields();

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        TitledAjaxSubmitLink submitButton = createSubmitButton(ldapsListPanel);
        form.add(submitButton);
        form.add(new DefaultButtonBehavior(submitButton));

        add(form);
    }

    private void addSearchFields(LdapSetting ldapDescriptor, TitledBorder borderDn) {
        searchPattern = ldapDescriptor.getSearch();
        if (searchPattern == null) {
            searchPattern = new SearchPattern();
        }

        borderDn.add(new TextField("searchFilter", new PropertyModel(searchPattern, "searchFilter")));
        borderDn.add(new SchemaHelpBubble("searchFilter.help", new SchemaHelpModel(searchPattern, "searchFilter")));

        borderDn.add(new TextField("searchBase", new PropertyModel(searchPattern, "searchBase")));
        borderDn.add(new SchemaHelpBubble("searchBase.help", new SchemaHelpModel(searchPattern, "searchBase")));

        borderDn.add(new StyledCheckbox("searchSubTree", new PropertyModel(searchPattern, "searchSubTree")));
        borderDn.add(new SchemaHelpBubble("searchSubTree.help", new SchemaHelpModel(searchPattern, "searchSubTree")));

        borderDn.add(new TextField("managerDn", new PropertyModel(searchPattern, "managerDn")));
        borderDn.add(new SchemaHelpBubble("managerDn.help", new SchemaHelpModel(searchPattern, "managerDn")));

        PasswordTextField managerPasswordField = new PasswordTextField(
                "managerPassword", new PropertyModel(searchPattern, "managerPassword"));
        managerPasswordField.setRequired(false);
        managerPasswordField.setResetPassword(false);
        borderDn.add(managerPasswordField);
        borderDn.add(new SchemaHelpBubble("managerPassword.help",
                new SchemaHelpModel(searchPattern, "managerPassword")));
    }

    private void addTestConnectionFields() {
        TitledBorder borderTest = new TitledBorder("borderTest");
        form.add(borderTest);
        borderTest.add(new TextField("testUsername", new PropertyModel(this, "testUsername")));
        borderTest.add(new HelpBubble("testUsername.help", "Username to test the ldap connection"));

        PasswordTextField testPasswordField = new PasswordTextField(
                "testPassword", new PropertyModel(this, "testPassword"));
        testPasswordField.setRequired(false);
        testPasswordField.setResetPassword(false);
        borderTest.add(testPasswordField);
        borderTest.add(new HelpBubble("testPassword.help", "Password to test the ldap connection"));

        // Test connection button
        borderTest.add(createTestConnectionButton());
    }

    private TitledAjaxSubmitLink createSubmitButton(final LdapsListPanel ldapsListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        TitledAjaxSubmitLink submit = new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                LdapSetting ldapSetting = (LdapSetting) form.getModelObject();
                if (!validateAndUpdateLdapSettings(ldapSetting)) {
                    AjaxUtils.refreshFeedback(target);
                    return;
                }

                MutableCentralConfigDescriptor configDescriptor = centralConfigService.getMutableDescriptor();
                if (isCreate()) {
                    configDescriptor.getSecurity().addLdap(ldapSetting);
                    centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
                    getPage().info("LDAP '" + entity.getKey() + "' successfully created.");
                } else {
                    configDescriptor.getSecurity().ldapSettingChanged(entity);
                    centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
                    getPage().info("LDAP '" + entity.getKey() + "' successfully updated.");
                }
                ldapsListPanel.setMutableDescriptor(configDescriptor);
                AjaxUtils.refreshFeedback(target);
                target.addComponent(ldapsListPanel);
                ModalHandler.closeCurrent(target);
            }
        };
        return submit;
    }

    private TitledAjaxSubmitLink createTestConnectionButton() {
        TitledAjaxSubmitLink submit = new TitledAjaxSubmitLink("testLdap", "Test Connection", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                LdapSetting ldapSetting = (LdapSetting) form.getModelObject();
                if (!validateAndUpdateLdapSettings(ldapSetting)) {
                    AjaxUtils.refreshFeedback(target);
                    return;
                }

                if (!StringUtils.hasText(testUsername) || !StringUtils.hasText(testUsername)) {
                    error("Please enter test username and password " +
                            "to test the LDAP settings");
                    return;
                }

                StatusHolder status = securityService.testLdapConnection(ldapSetting, testUsername, testPassword);

                if (status.isError()) {
                    error(status.getStatusMsg());
                } else {
                    info(status.getStatusMsg());
                }
                AjaxUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    private boolean validateAndUpdateLdapSettings(LdapSetting ldapSetting) {
        // validate userDnPattern or searchFilter
        boolean hasDnPattern = StringUtils.hasText(ldapSetting.getUserDnPattern());
        boolean hasSearch = StringUtils.hasText(searchPattern.getSearchFilter());
        if (!hasDnPattern && !hasSearch) {
            error("LDAP settings should provide a userDnPattern or a searchFilter (or both)");
            return false;
        }
        if (searchPattern.getSearchBase() == null) {
            searchPattern.setSearchBase("");
        }
        if (searchPattern.getSearchFilter() == null) {
            searchPattern.setSearchFilter("");
        }

        // if the search filter has value set the search pattern
        if (StringUtils.hasText(searchPattern.getSearchFilter())) {
            ldapSetting.setSearch(searchPattern);
        }

        return true;
    }


}