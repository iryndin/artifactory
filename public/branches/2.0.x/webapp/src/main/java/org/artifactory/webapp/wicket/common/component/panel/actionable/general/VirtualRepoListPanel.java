package org.artifactory.webapp.wicket.common.component.panel.actionable.general;

import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoResolver;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.model.FileActionableItem;
import org.artifactory.webapp.actionable.model.FolderActionableItem;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.panel.fieldset.FieldSetPanel;
import org.artifactory.webapp.wicket.utils.WebUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This panel displayes a list of the virtual repositories which are associated with the repo/of the selected item
 *
 * @author Noam Tenne
 */
public class VirtualRepoListPanel extends FieldSetPanel {
    public VirtualRepoListPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        add(new CssClass("horizontal-list"));
        addRepoList(repoItem);
    }

    /**
     * Adds a list of virtual repositories, using a RepoAwareActionableItem for information
     *
     * @param item The selected item from the tree
     */
    private void addRepoList(final RepoAwareActionableItem item) {
        List<VirtualRepoDescriptor> reposToDisplay = new ArrayList<VirtualRepoDescriptor>();
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        List<VirtualRepoDescriptor> virtualRepos = repositoryService.getVirtualRepoDescriptors();
        RepoDescriptor descriptor;
        //VirtualRepoResolver does not directly support local cache repos, so if the items descriptor is a cache,
        //We extract the caches remote repo, and use it insted
        if (item.getRepo() instanceof LocalCacheRepoDescriptor) {
            descriptor = ((LocalCacheRepoDescriptor) item.getRepo()).getRemoteRepo();
        } else {
            descriptor = item.getRepo();
        }
        for (VirtualRepoDescriptor virtualRepo : virtualRepos) {
            VirtualRepoResolver resolver = new VirtualRepoResolver(virtualRepo);
            if (resolver.contains(descriptor)) {
                reposToDisplay.add(virtualRepo);
            }
        }
        add(new ListView("items", reposToDisplay) {
            @Override
            protected void populateItem(ListItem virtualRepo) {
                VirtualRepoDescriptor virtualRepoDescriptor = (VirtualRepoDescriptor) virtualRepo.getModelObject();
                final String hrefPrefix = WebUtils.getWicketServletContextUrl();
                String path = getRepoPath(item);
                String href = hrefPrefix + "/" + virtualRepoDescriptor.getKey() + "/" + path;
                if (!href.endsWith("/")) {
                    href += "/";
                }
                AbstractLink link = new ExternalLink("link", href, virtualRepoDescriptor.getKey());
                link.add(new CssClass("repository-virtual"));
                virtualRepo.add(link);
            }
        });
    }

    private String getRepoPath(RepoAwareActionableItem item) {
        String path;
        if (item instanceof FolderActionableItem) {
            // get the full path of folders in case the folder item is compacted
            FolderActionableItem folderItem = (FolderActionableItem) item;
            return folderItem.getFolder().getRelPath();
        }
        if (item instanceof FileActionableItem) {
            // for files link to the parent folder
            return PathUtils.getParent(item.getRepoPath().getPath());
        }

        return item.getRepoPath().getPath();
    }

    @Override
    public String getTitle() {
        return "Virtual Repositories Association";
    }
}
