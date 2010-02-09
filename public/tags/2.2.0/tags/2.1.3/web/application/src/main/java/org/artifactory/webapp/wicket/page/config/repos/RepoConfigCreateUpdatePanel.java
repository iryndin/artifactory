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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Base panel for repositories configuration.
 *
 * @author Yossi Shaul
 */
public abstract class RepoConfigCreateUpdatePanel<E extends RepoDescriptor> extends CreateUpdatePanel<E> {

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private AddonsManager addons;

    private final MutableCentralConfigDescriptor mutableDescriptor;

    protected RepoConfigCreateUpdatePanel(CreateUpdateAction action, E repoDescriptor,
            MutableCentralConfigDescriptor mutableDescriptor) {
        super(action, repoDescriptor);
        this.mutableDescriptor = mutableDescriptor;

        setWidth(560);

        TitledBorder commonFields = new TitledBorder("commonFields");
        form.add(commonFields);

        // Repository name
        RequiredTextField repoKeyField = new RequiredTextField("key");
        repoKeyField.setEnabled(isCreate());// don't allow key update
        if (isCreate()) {
            repoKeyField.add(new JcrNameValidator("Invalid repository key '%s'."));
            repoKeyField.add(new XsdNCNameValidator("Invalid repository key '%s'."));
            repoKeyField.add(new UniqueXmlIdValidator(mutableDescriptor));
        }

        commonFields.add(repoKeyField);
        commonFields.add(new SchemaHelpBubble("key.help"));

        // Repository description
        commonFields.add(new TextArea("description"));
        commonFields.add(new SchemaHelpBubble("description.help"));

        // Cancel button
        form.add(new ModalCloseLink("cancel"));

        // Submit button
        TitledAjaxSubmitLink submit = createSubmitButton();
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));

        add(form);
    }

    public abstract void addDescriptor(MutableCentralConfigDescriptor mccd, E repoDescriptor);

    private TitledAjaxSubmitLink createSubmitButton() {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                E repoDescriptor = getRepoDescriptor();
                if (isCreate()) {
                    addDescriptor(mutableDescriptor, repoDescriptor);
                    centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
                    getPage().info("Repository '" + repoDescriptor.getKey() + "' successfully created.");
                } else {
                    centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
                    getPage().info("Repository '" + repoDescriptor.getKey() + "' successfully updated.");
                }

                ((RepositoryConfigPage) getPage()).refresh(target);
                AjaxUtils.refreshFeedback(target);
                close(target);
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    protected E getRepoDescriptor() {
        return (E) form.getModelObject();
    }

    protected List<RepoDescriptor> getRepos() {
        List<RepoDescriptor> result = new ArrayList<RepoDescriptor>();
        result.addAll(mutableDescriptor.getLocalRepositoriesMap().values());
        result.addAll(mutableDescriptor.getRemoteRepositoriesMap().values());
        result.addAll(mutableDescriptor.getVirtualRepositoriesMap().values());
        return result;
    }

    /**
     * Returns the addons provider
     *
     * @return AddonsProvider
     */
    protected AddonsManager getAddonsProvider() {
        return addons;
    }
}