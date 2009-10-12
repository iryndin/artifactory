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

package org.artifactory.webapp.wicket.page.config.proxy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
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

    public ProxyCreateUpdatePanel(CreateUpdateAction action, ProxyDescriptor proxyDescriptor,
            ProxiesListPanel proxiesListPanel) {
        super(action, proxyDescriptor);
        setWidth(380);

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
        border.add(new RequiredTextField("port", Integer.class));
        border.add(new TextField("username"));
        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        border.add(passwordField);

        border.add(new TextField("ntHost"));
        border.add(new TextField("domain"));

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
                    centralConfigService.saveEditedDescriptorAndReload(mutableCentralConfig);
                    getPage().info("Proxy '" + entity.getKey() + "' successfully created.");
                } else {
                    centralConfigService.saveEditedDescriptorAndReload(mutableCentralConfig);
                    getPage().info("Proxy '" + entity.getKey() + "' successfully updated.");
                }
                AjaxUtils.refreshFeedback(target);
                target.addComponent(proxiesListPanel);
                ModalHandler.closeCurrent(target);
            }
        };
    }

}