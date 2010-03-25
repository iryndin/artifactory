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

import org.apache.commons.collections15.OrderedMap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WebstartWebAddon;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.dnd.select.DragDropSelection;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoResolver;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.components.IconDragDropSelection;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Virtual repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class VirtualRepoPanel extends RepoConfigCreateUpdatePanel<VirtualRepoDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(VirtualRepoPanel.class);


    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private RepositoryService repositoryService;

    public VirtualRepoPanel(CreateUpdateAction action, VirtualRepoDescriptor repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);

        addBasicSettings(cachingDescriptorHelper);

        addAdvancedSettings(repoDescriptor);
    }

    @Override
    public void addAndSaveDescriptor(VirtualRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        mccd.addVirtualRepository(repoDescriptor);
        helper.syncAndSaveVirtualRepositories();
    }

    @Override
    public void saveEditDescriptor(VirtualRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        //update the model being saved
        OrderedMap<String, VirtualRepoDescriptor> virtualRepos =
                helper.getModelMutableDescriptor().getVirtualRepositoriesMap();
        if (virtualRepos.containsKey(repoDescriptor.getKey())) {
            virtualRepos.put(repoDescriptor.getKey(), repoDescriptor);
        }
        helper.syncAndSaveVirtualRepositories();
    }

    private void addBasicSettings(CachingDescriptorHelper cachingDescriptorHelper) {
        TitledBorder basicSettings = new TitledBorder("basicSettings");
        form.add(basicSettings);

        basicSettings.add(new RepoGeneralSettingsPanel("generalSettings", isCreate(), cachingDescriptorHelper));

        // resolved repos
        final WebMarkupContainer resolvedRepo = new WebMarkupContainer("resolvedRepoWrapper");
        resolvedRepo.setOutputMarkupId(true);
        basicSettings.add(resolvedRepo);
        basicSettings.add(new HelpBubble("resolvedRepo.help", new ResourceModel("resolvedRepo.help")));

        resolvedRepo.add(new DataView("resolvedRepo", new ResolvedReposDataProvider()) {
            @Override
            protected void populateItem(final Item item) {
                RepoDescriptor repo = (RepoDescriptor) item.getModelObject();
                item.add(new Label("key", repo.getKey()));
            }
        });

        // repositories
        List<RepoDescriptor> repos = getReposExcludingCurrent();
        DragDropSelection<RepoDescriptor> reposSelection =
                new IconDragDropSelection<RepoDescriptor>("repositories", repos) {
                    @Override
                    protected void onOrderChanged(AjaxRequestTarget target) {
                        super.onOrderChanged(target);
                        target.addComponent(resolvedRepo);
                        ModalHandler.resizeCurrent(target);
                    }
                };
        basicSettings.add(reposSelection);
        basicSettings.add(new SchemaHelpBubble("repositories.help"));
    }

    private void addAdvancedSettings(VirtualRepoDescriptor repoDescriptor) {
        TitledBorder advancedSettings = new TitledBorder("advancedSettings");
        advancedSettings.add(new CssClass("virtual-repo-panel-advanced-settings"));
        advancedSettings.add(new CollapsibleBehavior().setResizeModal(true));
        form.add(advancedSettings);

        // artifactoryRequestsCanRetrieveRemoteArtifacts
        advancedSettings.add(new StyledCheckbox("artifactoryRequestsCanRetrieveRemoteArtifacts"));
        advancedSettings.add(new SchemaHelpBubble("artifactoryRequestsCanRetrieveRemoteArtifacts.help"));

        // pomRepositoryReferencesCleanupPolicy
        PomCleanupPolicy[] policies = PomCleanupPolicy.values();
        DropDownChoice pomCleanPolicy =
                new DropDownChoice("pomRepositoryReferencesCleanupPolicy", Arrays.asList(policies),
                        new ChoiceRenderer("message"));
        advancedSettings.add(pomCleanPolicy);
        advancedSettings.add(new SchemaHelpBubble("pomRepositoryReferencesCleanupPolicy.help"));

        // keyPair
        WebstartWebAddon webstartAddon = addonsManager.addonByType(WebstartWebAddon.class);
        advancedSettings
                .add(webstartAddon.getKeyPairContainer("keyPairContainer", repoDescriptor.getKey(), isCreate()));

        advancedSettings.add(new TextArea("notes"));
        advancedSettings.add(new SchemaHelpBubble("notes.help"));
        addCleanCache(advancedSettings, repoDescriptor);
    }

    private void addCleanCache(TitledBorder advancedSettings, final VirtualRepoDescriptor descriptor) {
        TitledAjaxSubmitLink cleanCacheButton =
                new TitledAjaxSubmitLink("cleanCache", new Model("Zap Caches"), form) {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        String repoKey = descriptor.getKey();
                        RepoPath repoPath = RepoPath.repoRootPath(repoKey);
                        StatusHolder statusHolder;
                        statusHolder = repositoryService.undeploy(repoPath, false);
                        if (!statusHolder.isError()) {
                            info("The caches of '" + repoKey + "' have been successfully zapped.");
                        } else {
                            String message = "Could not zap caches for the virtual repository '" + repoKey + "': " +
                                    statusHolder.getStatusMsg() + ".";
                            error(message);
                        }
                        AjaxUtils.refreshFeedback(target);
                    }
                };
        cleanCacheButton.setVisible(!isCreate());
        advancedSettings.add(cleanCacheButton);
        HelpBubble help = new HelpBubble("cleanCache.help",
                "Clears all caches that are stored on the virtual repository level\n" +
                        "(transformed POMs, JNLP files, merged indexes, etc.)");
        help.setVisible(!isCreate());
        advancedSettings.add(help);
    }

    private List<RepoDescriptor> getReposExcludingCurrent() {
        // get all the list of available repositories excluding the current virtual repo
        List<RepoDescriptor> repos = getRepos();
        if (!isCreate()) {
            repos.remove(getRepoDescriptor());
        }
        return repos;
    }

    private class ResolvedReposDataProvider implements IDataProvider {
        public Iterator iterator(int first, int count) {
            VirtualRepoResolver resolver = new VirtualRepoResolver(getRepoDescriptor());
            return resolver.getOrderedRepos().iterator();
        }

        public int size() {
            VirtualRepoResolver resolver = new VirtualRepoResolver(getRepoDescriptor());
            return resolver.getOrderedRepos().size();
        }

        public IModel model(Object object) {
            return new Model((Serializable) object);
        }

        public void detach() {

        }
    }
}