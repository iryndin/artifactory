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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.PropertiesAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.links.ModalCloseLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Base panel for repositories configuration.
 *
 * @author Yossi Shaul
 */
public abstract class RepoConfigCreateUpdatePanel<E extends RepoDescriptor> extends CreateUpdatePanel<E> {

    @SpringBean
    protected CentralConfigService centralConfigService;

    @SpringBean
    private AddonsManager addons;

    private final CachingDescriptorHelper cachingDescriptorHelper;

    protected RepoConfigCreateUpdatePanel(CreateUpdateAction action, E repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor);
        this.cachingDescriptorHelper = cachingDescriptorHelper;

        add(new CssClass("repo-config"));
        setWidth(560);

        // Cancel button
        form.add(getCloseLink());

        // Submit button
        TitledAjaxSubmitLink submit = createSubmitButton();
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));

        add(form);
    }

    protected ModalCloseLink getCloseLink() {
        return new ModalCloseLink("cancel");
    }

    public abstract void addAndSaveDescriptor(E repoDescriptor);

    public abstract void saveEditDescriptor(E repoDescriptor);

    @SuppressWarnings({"unchecked"})
    protected E getRepoDescriptor() {
        return (E) form.getDefaultModelObject();
    }

    private TitledAjaxSubmitLink createSubmitButton() {
        String submitCaption = isCreate() ? "Create" : "Save";
        return new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                E repoDescriptor = getRepoDescriptor();
                if (isCreate()) {
                    addAndSaveDescriptor(repoDescriptor);
                    getPage().info("Repository '" + repoDescriptor.getKey() + "' successfully created.");
                } else {
                    saveEditDescriptor(repoDescriptor);
                    getPage().info("Repository '" + repoDescriptor.getKey() + "' successfully updated.");
                }

                ((RepositoryConfigPage) getPage()).refresh(target);
                AjaxUtils.refreshFeedback(target);
                close(target);
            }
        };
    }

    protected void addPropertySetsSettings(CachingDescriptorHelper cachingDescriptorHelper) {
        PropertiesAddon propertiesAddon = getAddonsProvider().addonByType(PropertiesAddon.class);
        final List<PropertySet> propertySets = cachingDescriptorHelper.getModelMutableDescriptor().getPropertySets();
        final RealRepoDescriptor repoDescriptor = (RealRepoDescriptor) entity;
        form.add(propertiesAddon.getPropertySetsBorder("propertySetsBorder", "propertySets", repoDescriptor,
                propertySets));
    }

    protected CachingDescriptorHelper getCachingDescriptorHelper() {
        return cachingDescriptorHelper;
    }

    protected List<RepoDescriptor> getRepos() {
        List<RepoDescriptor> result = new ArrayList<RepoDescriptor>();
        MutableCentralConfigDescriptor mutableDescriptor = cachingDescriptorHelper.getModelMutableDescriptor();
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