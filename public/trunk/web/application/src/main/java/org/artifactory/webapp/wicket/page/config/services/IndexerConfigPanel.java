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

package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.NumberValidator;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.index.IndexerService;
import org.artifactory.common.wicket.component.CancelLink;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.dnd.select.sorted.SortedDragDropSelection;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledActionPanel;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.components.SortedRepoDragDropSelection;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * General settings (server name, max upload, etc.) configuration panel.
 *
 * @author Yossi Shaul
 */
public class IndexerConfigPanel extends TitledActionPanel {
    private static final Logger log = LoggerFactory.getLogger(IndexerConfigPanel.class);

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private IndexerService indexerService;

    private IndexerDescriptor indexer;

    public IndexerConfigPanel(String id, Form form) {
        super(id);

        CentralConfigDescriptor centralConfig = centralConfigService.getDescriptor();
        indexer = centralConfig.getIndexer();
        if (indexer == null) {
            indexer = new IndexerDescriptor();
        }

        setModel(new CompoundPropertyModel(indexer));

        add(new StyledCheckbox("enabled"));
        add(new SchemaHelpBubble("enabled.help"));

        final TextField indexingIntervalHours = new TextField("indexingIntervalHours", Integer.class);
        indexingIntervalHours.add(new NumberValidator.MinimumValidator(1));
        add(indexingIntervalHours);
        add(new SchemaHelpBubble("indexingIntervalHours.help"));

        // add the run link
        TitledAjaxLink runLink = new TitledAjaxLink("run", "Run Indexing Now") {
            public void onClick(AjaxRequestTarget target) {
                MultiStatusHolder statusHolder = new MultiStatusHolder();
                try {
                    indexerService.scheduleImmediateIndexing();
                    info("Indexer was successfully scheduled to run in the background.");
                } catch (Exception e) {
                    log.error("Could not run indexer.", e);
                    statusHolder.setError(e.getMessage(), log);
                    error("Indexer did not run: " + e.getMessage());
                }
            }
        };
        add(runLink);

        List<RepoDescriptor> repoSet = new ArrayList<RepoDescriptor>();
        repoSet.addAll(repositoryService.getLocalAndRemoteRepoDescriptors());
        repoSet.addAll(getFilteredVirtualRepoDescriptors());

        SortedDragDropSelection<RepoDescriptor> selection =
                new SortedRepoDragDropSelection<RepoDescriptor>("excludedRepositories", repoSet) {
                    @Override
                    protected Collection<RepoDescriptor> createNewSelectionCollection(int length) {
                        //Return a set instead of a list
                        return new TreeSet<RepoDescriptor>();
                    }
                };
        add(selection);
        add(new SchemaHelpBubble("excludedRepositories.help"));

        addDefaultButton(createSaveButton(form));
        addButton(new CancelLink(form));
    }

    @Override
    public String getTitle() {
        return "Indexer Configuration";
    }

    @Override
    protected Component newToolbar(String id) {
        return new HelpBubble(id, new ResourceModel("indexerConfig.help"));
    }

    private TitledAjaxSubmitLink createSaveButton(Form form) {
        return new TitledAjaxSubmitLink("save", "Save", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // get the indexer configuration and if valid
                IndexerDescriptor indexer = IndexerConfigPanel.this.indexer;
                MutableCentralConfigDescriptor ccDescriptor = centralConfigService.getMutableDescriptor();
                ccDescriptor.setIndexer(indexer);
                centralConfigService.saveEditedDescriptorAndReload(ccDescriptor);
                info("Indexer service settings successfully updated.");
                AjaxUtils.refreshFeedback(target);
                target.addComponent(this);
            }
        };
    }

    /**
     * Returns all the virtual repositories, apart from the global one
     *
     * @return Virtual repo descriptor list
     */
    public List<VirtualRepoDescriptor> getFilteredVirtualRepoDescriptors() {
        List<VirtualRepoDescriptor> virtualRepoDescriptors = repositoryService.getVirtualRepoDescriptors();
        List<VirtualRepoDescriptor> descriptorsToAdd = new ArrayList<VirtualRepoDescriptor>();
        for (VirtualRepoDescriptor descriptorToCheck : virtualRepoDescriptors) {
            if (!descriptorToCheck.getKey().equals(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY)) {
                descriptorsToAdd.add(descriptorToCheck);
            }
        }
        return descriptorsToAdd;
    }
}
