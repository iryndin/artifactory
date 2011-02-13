/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config.layout;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidator;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.security.AccessLogger;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.layout.validators.LayoutFieldRequiredTokenValidator;
import org.artifactory.webapp.wicket.page.config.layout.validators.ReservedLayoutNameValidator;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;

/**
 * @author Yoav Aharoni
 */
public class LayoutCreateUpdatePanel extends CreateUpdatePanel<RepoLayout> {

    @SpringBean
    private CentralConfigService centralConfigService;

    @WicketProperty
    private String pathToTest;

    private LayoutListPanel layoutListPanel;

    public LayoutCreateUpdatePanel(CreateUpdateAction action, RepoLayout layout, LayoutListPanel layoutListPanel) {
        super(action, layout);
        this.layoutListPanel = layoutListPanel;

        add(form);

        TitledBorder layoutFields = new TitledBorder("formBorder");
        form.add(layoutFields);

        boolean disableFields = !isCreate() && RepoLayoutUtils.isReservedName(layout.getName());

        FormComponent<String> nameTextField = new TextField<String>("name");
        if (isCreate()) {
            nameTextField.add(new JcrNameValidator("Invalid layout name '%s'."));
            nameTextField.add(new XsdNCNameValidator("Invalid layout name '%s'."));
            nameTextField.add(new UniqueXmlIdValidator(layoutListPanel.getMutableDescriptor()));
            nameTextField.add(new ReservedLayoutNameValidator());
        } else {
            nameTextField.setEnabled(false);
        }
        layoutFields.add(nameTextField);
        layoutFields.add(new SchemaHelpBubble("name.help"));

        addLayoutField("artifactPathPattern", layoutFields, true, disableFields,
                new LayoutFieldRequiredTokenValidator());

        final FormComponent<String> descriptorPathPatternTextField =
                new ReadOnlyOnDisabledTextField<String>("descriptorPathPattern").setRequired(true);
        descriptorPathPatternTextField.setEnabled(entity.isDistinctiveDescriptorPathPattern() && !disableFields);
        descriptorPathPatternTextField.setRequired(entity.isDistinctiveDescriptorPathPattern());
        descriptorPathPatternTextField.add(new LayoutFieldRequiredTokenValidator());

        descriptorPathPatternTextField.setOutputMarkupId(true);
        layoutFields.add(descriptorPathPatternTextField);

        final StyledCheckbox distinctiveDescriptorPathPatternCheckBox =
                new StyledCheckbox("distinctiveDescriptorPathPattern");
        distinctiveDescriptorPathPatternCheckBox.setEnabled(!disableFields);
        distinctiveDescriptorPathPatternCheckBox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean enable = entity.isDistinctiveDescriptorPathPattern();
                descriptorPathPatternTextField.setEnabled(enable);
                descriptorPathPatternTextField.setRequired(enable);
                target.addComponent(descriptorPathPatternTextField);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new NoAjaxIndicatorDecorator();
            }
        });
        layoutFields.add(distinctiveDescriptorPathPatternCheckBox);
        layoutFields.add(new SchemaHelpBubble("distinctiveDescriptorPathPattern.help"));

        addLayoutField("folderIntegrationRevisionRegExp", layoutFields, true, disableFields);
        addLayoutField("fileIntegrationRevisionRegExp", layoutFields, true, disableFields);

        addTestBorder();

        // add the panel display which shows the association of the layout with the repositories.
        if (isCreate()) {
            form.add(new WebMarkupContainer("repoList").setVisible(false));
        } else {
            RepositoriesListPanel repositoriesListPanel = new RepositoriesListPanel("repoList", layout);
            repositoriesListPanel.add(new CollapsibleBehavior().setUseAjax(true).setResizeModal(true));
            form.add(repositoriesListPanel);
        }

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        TitledAjaxSubmitLink submit = createSubmitButton();
        submit.setEnabled(!disableFields);
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    private TitledAjaxSubmitLink createSubmitButton() {
        return new TitledAjaxSubmitLink("submit", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                MutableCentralConfigDescriptor configDescriptor = layoutListPanel.getMutableDescriptor();

                if (StringUtils.isBlank(entity.getName())) {
                    error("Please enter a valid layout name.");
                    AjaxUtils.refreshFeedback(target);
                    return;
                }

                if (isCreate()) {
                    configDescriptor.addRepoLayout(entity);
                    centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
                    String message = "Layout '" + entity.getName() + "' successfully created.";
                    AccessLogger.created(message);
                    getPage().info(message);
                } else {
                    centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
                    String message = "Layout '" + entity.getName() + "' successfully updated.";
                    AccessLogger.updated(message);
                    getPage().info(message);
                }
                AjaxUtils.refreshFeedback(target);
                target.addComponent(layoutListPanel);
                close(target);
            }
        };
    }

    private TextField<String> addLayoutField(String id, MarkupContainer titledBorder, boolean required,
            boolean disableFields, IValidator<String>... validators) {
        TextField<String> textField = new ReadOnlyOnDisabledTextField<String>(id);
        textField.setRequired(required).setOutputMarkupId(true).setEnabled(!disableFields);
        textField.add(validators);
        titledBorder.add(textField);
        titledBorder.add(new SchemaHelpBubble(id + ".help"));

        return textField;
    }

    private void addTestBorder() {
        final TitledBorder testBorder = new TitledBorder("testBorder", new CompoundPropertyModel(new ModuleInfo()));
        form.add(testBorder);

        final Label organization = new Label("organization");
        organization.setVisible(false);
        testBorder.add(organization);

        testBorder.add(new Label("module"));
        testBorder.add(new Label("baseRevision"));
        testBorder.add(new Label("folderIntegrationRevision"));
        testBorder.add(new Label("fileIntegrationRevision"));
        testBorder.add(new Label("classifier"));
        testBorder.add(new Label("ext"));
        testBorder.add(new Label("type"));

        testBorder.add(new TextField<String>("pathToTest", new PropertyModel<String>(this, "pathToTest")));

        TitledAjaxSubmitLink testPatternsButton = new TitledAjaxSubmitLink("testPatterns", "Test", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (StringUtils.isBlank(pathToTest)) {
                    error("Please enter a path to test.");
                    AjaxUtils.refreshFeedback();
                    return;
                }
                try {
                    ModuleInfo moduleInfo = null;
                    if (entity.isDistinctiveDescriptorPathPattern()) {
                        moduleInfo = ModuleInfoUtils.moduleInfoFromDescriptorPath(pathToTest, entity);
                    }
                    if ((moduleInfo == null) || !moduleInfo.isValid()) {
                        moduleInfo = ModuleInfoUtils.moduleInfoFromArtifactPath(pathToTest, entity);
                    }
                    testBorder.setDefaultModelObject(moduleInfo);
                    organization.setVisible(true);
                    target.addComponent(testBorder);
                } catch (Exception e) {
                    error("Failed to test path: " + ExceptionUtils.getRootCause(e).getMessage());
                }
                ModalHandler.resizeAndCenterCurrent(target);
                AjaxUtils.refreshFeedback();
            }
        };

        testBorder.add(testPatternsButton);
    }

    private static class ReadOnlyOnDisabledTextField<T> extends TextField<T> {

        public ReadOnlyOnDisabledTextField(String id) {
            super(id);
        }

        @Override
        protected void onDisabled(ComponentTag tag) {
            tag.put("readonly", "readonly");
        }
    }
}
