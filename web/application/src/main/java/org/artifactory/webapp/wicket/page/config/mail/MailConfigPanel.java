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

package org.artifactory.webapp.wicket.page.config.mail;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.mail.MailServerConfiguration;
import org.artifactory.api.mail.MailService;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.EmailException;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.util.validation.PortNumberValidator;
import org.slf4j.Logger;

/**
 * Displays the different fields required for the mail server configuration
 *
 * @author Noam Tenne
 */
public class MailConfigPanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(MailConfigPanel.class);

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private MailService mailService;

    private TextField testRecipientTextField;

    private Form form;

    public MailConfigPanel(String id) {
        super(id);
        MailServerDescriptor descriptor = getMailServerDescriptor();
        CompoundPropertyModel compoundPropertyModel = new CompoundPropertyModel(descriptor);
        form = new Form("form", compoundPropertyModel);

        addField("host", null, true, false, null, descriptor);
        final TextField portTextField = addField("port", null, true, true, new PortNumberValidator(), descriptor);
        addField("username", null, false, false, null, descriptor);
        PasswordTextField passwordTextField =
                new PasswordTextField("password", new PropertyModel(descriptor, "password"));
        passwordTextField.setResetPassword(false);
        passwordTextField.setRequired(false);
        form.add(passwordTextField);
        addField("from", null, false, false, EmailAddressValidator.getInstance(), descriptor);
        addField("subjectPrefix", null, false, false, null, descriptor);
        form.add(new StyledCheckbox("tls", new PropertyModel(descriptor, "tls")));
        final StyledCheckbox sslCheckbox = new StyledCheckbox("ssl", new PropertyModel(descriptor, "ssl"));

        //Add behavior that auto-switches the port to default SSL or normal values
        sslCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (sslCheckbox.isChecked()) {
                    portTextField.setModelObject(465);
                } else {
                    portTextField.setModelObject(25);
                }
                target.addComponent(portTextField);
            }
        });
        form.add(sslCheckbox);

        form.add(new SchemaHelpBubble("password.help"));
        form.add(new SchemaHelpBubble("tls.help"));
        form.add(new SchemaHelpBubble("ssl.help"));

        TitledBorder borderTest = new TitledBorder("testBorder");
        form.add(borderTest);

        testRecipientTextField = new TextField("testRecipient", new Model());
        testRecipientTextField.add(EmailAddressValidator.getInstance());
        borderTest.add(testRecipientTextField);
        borderTest.add(createSendTestButton(form));

        add(form);
    }

    @Override
    public String getTitle() {
        return "Mail Server Settings";
    }

    /**
     * Creates the form save button
     *
     * @return TitledAjaxSubmitLink - The save button
     */
    public TitledAjaxSubmitLink createSaveButton() {
        TitledAjaxSubmitLink saveButton = new TitledAjaxSubmitLink("save", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                MutableCentralConfigDescriptor cc = centralConfigService.getMutableDescriptor();
                cc.setMailServer(((MailServerDescriptor) form.getModelObject()));
                centralConfigService.saveEditedDescriptorAndReload(cc);
                info("Mail server settings successfully updated.");
                AjaxUtils.refreshFeedback(target);
            }
        };
        form.add(new DefaultButtonBehavior(saveButton));
        return saveButton;
    }

    /**
     * Creates the send test mail button
     *
     * @param form The panel's main form
     * @return TitledAjaxSubmitLink - The send button
     */
    private TitledAjaxSubmitLink createSendTestButton(Form form) {
        TitledAjaxSubmitLink searchButton = new TitledAjaxSubmitLink("sendTest", "Send Test Mail", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String testRecipient = testRecipientTextField.getValue();
                if (StringUtils.isEmpty(testRecipient)) {
                    displayError(target, "Please specify a recipient for the test message.");
                    return;
                }
                MailServerDescriptor descriptor = (MailServerDescriptor) form.getModelObject();
                MailServerConfiguration mailServerConfiguration = new MailServerConfiguration(descriptor);
                if (!validateConfig(mailServerConfiguration)) {
                    displayError(target, "Sending a test message requires host and port properties, at least.");
                    return;
                }

                sendMail(target, mailServerConfiguration);
            }

            public boolean validateConfig(MailServerConfiguration mailServerConfiguration) {
                boolean hasHost = false;
                boolean hasPort = false;
                if (mailServerConfiguration != null) {
                    hasHost = !StringUtils.isEmpty(mailServerConfiguration.getHost());
                    hasPort = (mailServerConfiguration.getPort() > 0);
                }

                return hasHost && hasPort;
            }

            private void sendMail(AjaxRequestTarget target, MailServerConfiguration configuration) {
                String testRecipient = testRecipientTextField.getValue();
                //Sanity check (has validator): If the recipient field is empty, alert
                if (!StringUtils.isEmpty(testRecipient)) {
                    try {
                        mailService.sendMail(new String[]{testRecipient}, "Test",
                                "This is a test message from Artifactory", configuration);
                        String confirmMessage = "A test message has been sent successfully to '" + testRecipient + "'";
                        info(confirmMessage);
                        log.info(confirmMessage);
                        AjaxUtils.refreshFeedback(target);
                    } catch (EmailException e) {
                        String message = e.getMessage();
                        if (message == null) {
                            message = "An error has occured while sending an e-mail.";
                        }
                        log.error(message, e);
                        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                        message += " Please review the <a href=\"" + systemLogsPage + "\">log</a> for further details.";
                        displayError(target, message);
                    }
                } else {
                    displayError(target, "Test recipient field cannot be empty");
                }
            }

            private void displayError(AjaxRequestTarget target, String error) {
                error(error);
                AjaxUtils.refreshFeedback(target);
            }
        };
        return searchButton;
    }

    /**
     * Returns the mail server descriptor
     *
     * @return MailServerDescriptor - The mail server descriptor
     */
    private MailServerDescriptor getMailServerDescriptor() {
        CentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
        MailServerDescriptor mailServerDescriptor = centralConfig.getMailServer();
        if (mailServerDescriptor == null) {
            mailServerDescriptor = new MailServerDescriptor();
        }
        return mailServerDescriptor;
    }

    /**
     * Adds a new text field and a help bubble
     *
     * @param id             The ID the new text field will recieve
     * @param type           The Class of the input type - can be null
     * @param required       Is the field required
     * @param outputMarkupId Should field output the markup id
     * @param validator      A validator to add to the field - can be null
     * @param descriptor     The descriptor
     * @return TextField - The newly created and added text field
     */
    private TextField addField(String id, Class type, boolean required, boolean outputMarkupId, IValidator validator,
            Descriptor descriptor) {
        TextField textField = (type != null) ? new TextField(id, type) : new TextField(id);
        textField.setOutputMarkupId(outputMarkupId);
        textField.setRequired(required);
        if (validator != null) {
            textField.add(validator);
        }
        textField.setModel(new PropertyModel(descriptor, id));

        form.add(textField);
        form.add(new SchemaHelpBubble(id + ".help"));

        return textField;
    }
}