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
package org.artifactory.webapp.wicket.page.config.proxy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalCloseLink;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.utils.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.utils.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.utils.validation.XsdNCNameValidator;

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
        setWidth(350);

        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        // Proxy key
        RequiredTextField proxyKeyField = new RequiredTextField("key");
        proxyKeyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            proxyKeyField.add(JcrNameValidator.getInstance());
            proxyKeyField.add(XsdNCNameValidator.getInstance());
            proxyKeyField.add(new UniqueXmlIdValidator(getEditingDescriptor()));
        }
        border.add(proxyKeyField);

        border.add(new RequiredTextField("host"));
        border.add(new RequiredTextField("port", Integer.class));
        border.add(new TextField("username"));
        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        border.add(passwordField);

        border.add(new TextField("domain"));

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        SimpleButton submit = createSubmitButton(proxiesListPanel);
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));

        border.add(new SchemaHelpBubble("key.help"));
        border.add(new SchemaHelpBubble("host.help"));
        border.add(new SchemaHelpBubble("port.help"));
        border.add(new SchemaHelpBubble("username.help"));
        border.add(new SchemaHelpBubble("password.help"));
        border.add(new SchemaHelpBubble("domain.help"));

    }

    private SimpleButton createSubmitButton(final ProxiesListPanel proxiesListPanel) {
        String submitCaption = isCreate() ? "Create" : "Save";
        SimpleButton submit = new SimpleButton("submit", form, submitCaption) {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (isCreate()) {
                    getEditingDescriptor().addProxy(entity);
                    centralConfigService.saveEditedDescriptorAndReload();
                    info("Proxy '" + entity.getKey() + "' successfully created.");
                } else {
                    centralConfigService.saveEditedDescriptorAndReload();
                    info("Proxy '" + entity.getKey() + "' successfully updated.");
                }
                FeedbackUtils.refreshFeedback(target);
                target.addComponent(proxiesListPanel);
                ModalHandler.closeCurrent(target);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                FeedbackUtils.refreshFeedback(target);
            }
        };
        return submit;
    }

    protected MutableCentralConfigDescriptor getEditingDescriptor() {
        return centralConfigService.getDescriptorForEditing();
    }
}