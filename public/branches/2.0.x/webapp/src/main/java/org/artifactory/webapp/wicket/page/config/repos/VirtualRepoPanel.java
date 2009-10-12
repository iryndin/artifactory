package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoResolver;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.dnd.select.DragDropSelection;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * Virtual repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class VirtualRepoPanel extends RepoConfigCreateUpdatePanel<VirtualRepoDescriptor> {

    public VirtualRepoPanel(CreateUpdateAction action, VirtualRepoDescriptor repoDescriptor) {
        super(action, repoDescriptor);

        TitledBorder virtualRepoFields = new TitledBorder("virtualRepoFields");
        form.add(virtualRepoFields);

        virtualRepoFields.add(new StyledCheckbox("artifactoryRequestsCanRetrieveRemoteArtifacts"));
        virtualRepoFields.add(new SchemaHelpBubble("artifactoryRequestsCanRetrieveRemoteArtifacts.help"));

        final WebMarkupContainer resolvedRepo = new WebMarkupContainer("resolvedRepoWrapper");
        resolvedRepo.setOutputMarkupId(true);
        virtualRepoFields.add(resolvedRepo);
        virtualRepoFields.add(new HelpBubble("resolvedRepo.help",
                new ResourceModel("resolvedRepo.help")));

        resolvedRepo.add(new DataView("resolvedRepo", new ResolvedReposDataProvider()) {
            @Override
            protected void populateItem(final Item item) {
                RepoDescriptor repo = (RepoDescriptor) item.getModelObject();
                item.add(new Label("key", repo.getKey()));
            }
        });

        List<RepoDescriptor> repos = getReposExcludingCurrent();
        DragDropSelection<RepoDescriptor> reposSelection =
                new DragDropSelection<RepoDescriptor>("repositories", repos) {
                    @Override
                    protected void populateItem(ListItem item) {
                        super.populateItem(item);
                        String simpleName = item.getModelObject().getClass().getSimpleName();
                        item.add(new CssClass(simpleName));
                    }

                    @Override
                    protected void onOrderChanged(AjaxRequestTarget target) {
                        super.onOrderChanged(target);
                        target.addComponent(resolvedRepo);
                    }
                };
        virtualRepoFields.add(reposSelection);
        virtualRepoFields.add(new SchemaHelpBubble("repositories.help"));
    }

    @Override
    public void handleCreate(CentralConfigDescriptor descriptor) {
        VirtualRepoDescriptor virtualRepoDescriptor = getRepoDescriptor();
        getEditingDescriptor().addVirtualRepository(virtualRepoDescriptor);
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