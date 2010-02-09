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

package org.artifactory.webapp.wicket.page.config.proxy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.confirm.AjaxConfirm;
import org.artifactory.common.wicket.component.confirm.ConfirmDialog;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.PortNumberValidator;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;
import org.springframework.util.StringUtils;

/**
 * Proxies configuration panel.
 *
 * @author Yossi Shaul
 */
public class ProxyCreateUpdatePanel extends CreateUpdatePanel<ProxyDescriptor> {

    @SpringBean
    private CentralConfigService centralConfigService;

    private boolean defaultForAllRemotRepo;

    public ProxyCreateUpdatePanel(CreateUpdateAction action, ProxyDescriptor proxyDescriptor,
            ProxiesListPanel proxiesListPanel) {
        super(action, proxyDescriptor);
        setWidth(410);

        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        // Proxy key
        RequiredTextField proxyKeyField = new RequiredTextField("key");
        proxyKeyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            proxyKeyField.add(new JcrNameValidator("Invalid proxy key '%s'."));
            proxyKeyField.add(new XsdNCNameValidator("Invalid proxy key '%s'."));
            proxyKeyField.add(new UniqueXmlIdValidator(proxiesListPanel.getEditingDescriptor()));
        }
        border.add(proxyKeyField);

        border.add(new RequiredTextField("host"));

        RequiredTextField portField = new RequiredTextField("port");
        portField.add(new PortNumberValidator());
        border.add(portField);

        border.add(new TextField("username"));
        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        border.add(passwordField);

        border.add(new TextField("ntHost"));
        border.add(new TextField("domain"));

        //Global Proxy check box
        SystemDefaultCheckbox sysCheckbox = new SystemDefaultCheckbox(entity.isDefaultProxy());
        border.add(sysCheckbox);

        // Cancel button
        form.add(new ModalCloseLink("cancel"));
        // Submit button
        TitledAjaxSubmitLink submit = createSubmitButton(proxiesListPanel);
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));

        border.add(new SchemaHelpBubble("key.help"));
        border.add(new SchemaHelpBubble("host.help"));
        border.add(new SchemaHelpBubble("port.help"));
        border.add(new SchemaHelpBubble("username.help"));
        border.add(new SchemaHelpBubble("password.help"));
        border.add(new SchemaHelpBubble("ntHost.help"));
        border.add(new SchemaHelpBubble("domain.help"));
        border.add(new SchemaHelpBubble("defaultProxy.help"));
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    private TitledAjaxSubmitLink createSubmitButton(final ProxiesListPanel proxiesListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (StringUtils.hasText(entity.getDomain()) && !StringUtils.hasText(entity.getNtHost())) {
                    error("Please specify a NT host value to use with the NT domain.");
                    return;
                }
                MutableCentralConfigDescriptor mutableCentralConfig = proxiesListPanel.getEditingDescriptor();
                if (isCreate()) {
                    mutableCentralConfig.addProxy(entity);
                    getPage().info("Proxy '" + entity.getKey() + "' successfully created.");
                } else {
                    getPage().info("Proxy '" + entity.getKey() + "' successfully updated.");
                }
                mutableCentralConfig.proxyChanged(entity, defaultForAllRemotRepo);
                centralConfigService.saveEditedDescriptorAndReload(mutableCentralConfig);
                AjaxUtils.refreshFeedback(target);
                target.addComponent(proxiesListPanel);
                ModalHandler.closeCurrent(target);
            }
        };
    }

    private class SystemDefaultCheckbox extends StyledCheckbox {
        private SystemDefaultCheckbox(final boolean checked) {
            super("sysCheckbox", new Model(checked));
            setOutputMarkupId(true);

            add(new AjaxFormComponentUpdatingBehavior("onclick") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (isChecked()) {
                        AjaxConfirm.get().confirm(new ConfirmDialog() {
                            public String getMessage() {
                                return "Do you wish to use this proxy with existing remote repositories (and override any assigned proxies)?";
                            }

                            public void onConfirm(boolean approved, AjaxRequestTarget target) {
                                update(approved, target);
                            }
                        });
                    } else {
                        update(true, target);
                    }
                }
            });
        }

        private void update(boolean approved, AjaxRequestTarget target) {
            boolean checked = isChecked();
            defaultForAllRemotRepo = checked && approved;
            entity.setDefaultProxy(checked);
            target.addComponent(this);
        }
    }
}