package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.CancelDefaultDecorator;
import static org.artifactory.webapp.wicket.common.component.CreateUpdateAction.CREATE;
import static org.artifactory.webapp.wicket.common.component.CreateUpdateAction.UPDATE;
import org.artifactory.webapp.wicket.common.component.SimpleLink;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalShowLink;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.sortedlist.OrderedListPanel;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Repositories configuration page.
 *
 * @author Yossi Shaul
 */
@SuppressWarnings({"OverlyComplexAnonymousInnerClass"})
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class RepositoryConfigPage extends AuthenticatedPage {
    private static final Logger log = LoggerFactory.getLogger(RepositoryConfigPage.class);

    @SpringBean
    private CentralConfigService centralConfigService;
    private WebMarkupContainer reposListContainer;

    public RepositoryConfigPage() {
        reposListContainer = new WebMarkupContainer("reposList");
        reposListContainer.setOutputMarkupId(true);
        add(reposListContainer);

        addLocalReposList();
        addRemoteReposList();
        addVirtualReposList();
    }

    @Override
    protected String getPageName() {
        return "Configure Repositories";
    }

    private void addLocalReposList() {
        IModel repoListModel = new RepoListModel<LocalRepoDescriptor>() {
            @Override
            protected Collection<LocalRepoDescriptor> getRepos() {
                return getDescriptorForEditing().getLocalRepositoriesMap().values();
            }

            @Override
            protected void saveRepos(List<LocalRepoDescriptor> repos) {
                OrderedMap<String, LocalRepoDescriptor> currentLocalReposMap =
                        getDescriptorForEditing().getLocalRepositoriesMap();
                assertLegalReorderedList(repos, currentLocalReposMap.values());

                ListOrderedMap<String, LocalRepoDescriptor> localReposMap =
                        new ListOrderedMap<String, LocalRepoDescriptor>();
                for (LocalRepoDescriptor localRepo : repos) {
                    localReposMap.put(localRepo.getKey(), localRepo);
                }
                getDescriptorForEditing().setLocalRepositoriesMap(localReposMap);
                centralConfigService.saveEditedDescriptorAndReload();
            }
        };

        reposListContainer.add(new RepoListPanel<LocalRepoDescriptor>("localRepos", repoListModel) {
            @Override
            protected String getItemDisplayValue(LocalRepoDescriptor itemObject) {
                return itemObject.getKey();
            }

            @Override
            protected BaseModalPanel newCreateItemPanel() {
                return new LocalRepoPanel(CREATE, new LocalRepoDescriptor());
            }

            @Override
            protected BaseModalPanel newUpdateItemPanel(LocalRepoDescriptor itemObject) {
                return new LocalRepoPanel(UPDATE, itemObject);
            }


            @Override
            protected Component newToolbar(String id) {
                return new SchemaHelpBubble(id, getHelpModel("localRepositoriesMap"));
            }
        });
    }

    private void addRemoteReposList() {
        IModel repoListModel = new RepoListModel<RemoteRepoDescriptor>() {
            @Override
            protected Collection<RemoteRepoDescriptor> getRepos() {
                return getDescriptorForEditing().getRemoteRepositoriesMap().values();
            }

            @Override
            protected void saveRepos(List<RemoteRepoDescriptor> repos) {
                OrderedMap<String, RemoteRepoDescriptor> currentReposMap =
                        getDescriptorForEditing().getRemoteRepositoriesMap();
                assertLegalReorderedList(repos, currentReposMap.values());

                ListOrderedMap<String, RemoteRepoDescriptor> remoteReposMap =
                        new ListOrderedMap<String, RemoteRepoDescriptor>();
                for (RemoteRepoDescriptor remoteRepo : repos) {
                    remoteReposMap.put(remoteRepo.getKey(), remoteRepo);
                }
                getDescriptorForEditing().setRemoteRepositoriesMap(remoteReposMap);
                centralConfigService.saveEditedDescriptorAndReload();
            }
        };

        reposListContainer.add(new RepoListPanel<RemoteRepoDescriptor>("remoteRepos", repoListModel) {
            @Override
            protected String getItemDisplayValue(RemoteRepoDescriptor itemObject) {
                return itemObject.getKey();
            }

            @Override
            protected BaseModalPanel newCreateItemPanel() {
                return new HttpRepoPanel(CREATE, new HttpRepoDescriptor());
            }

            @Override
            protected HttpRepoPanel newUpdateItemPanel(RemoteRepoDescriptor itemObject) {
                return new HttpRepoPanel(UPDATE, (HttpRepoDescriptor) itemObject);
            }

            @Override
            protected Component newToolbar(String id) {
                return new SchemaHelpBubble(id, getHelpModel("remoteRepositoriesMap"));
            }
        });
    }


    private void addVirtualReposList() {
        IModel repoListModel = new RepoListModel<VirtualRepoDescriptor>() {
            @Override
            protected Collection<VirtualRepoDescriptor> getRepos() {
                return getDescriptorForEditing().getVirtualRepositoriesMap().values();
            }

            @Override
            protected void saveRepos(List<VirtualRepoDescriptor> repos) {
                OrderedMap<String, VirtualRepoDescriptor> currentReposMap =
                        getDescriptorForEditing().getVirtualRepositoriesMap();
                assertLegalReorderedList(repos, currentReposMap.values());

                ListOrderedMap<String, VirtualRepoDescriptor> virtualReposMap =
                        new ListOrderedMap<String, VirtualRepoDescriptor>();
                for (VirtualRepoDescriptor virtualRepo : repos) {
                    virtualReposMap.put(virtualRepo.getKey(), virtualRepo);
                }
                getDescriptorForEditing().setVirtualRepositoriesMap(virtualReposMap);
                centralConfigService.saveEditedDescriptorAndReload();
            }
        };

        reposListContainer.add(new RepoListPanel<VirtualRepoDescriptor>("virtualRepos", repoListModel) {
            @Override
            protected String getItemDisplayValue(VirtualRepoDescriptor itemObject) {
                return itemObject.getKey();
            }

            @Override
            protected BaseModalPanel newCreateItemPanel() {
                return new VirtualRepoPanel(CREATE, new VirtualRepoDescriptor());
            }

            @Override
            protected BaseModalPanel newUpdateItemPanel(VirtualRepoDescriptor itemObject) {
                return new VirtualRepoPanel(UPDATE, itemObject);
            }

            @Override
            protected Component newToolbar(String id) {
                return new SchemaHelpBubble(id, getHelpModel("virtualRepositoriesMap"));
            }
        });
    }

    private SchemaHelpModel getHelpModel(String property) {
        return new SchemaHelpModel(getDescriptorForEditing(), property);
    }

    public void refresh(AjaxRequestTarget target) {
        target.addComponent(reposListContainer);
    }

    private MutableCentralConfigDescriptor getDescriptorForEditing() {
        return centralConfigService.getDescriptorForEditing();
    }

    private class DeleteRepoLink extends SimpleLink {
        private final RepoDescriptor repoDescriptor;

        private DeleteRepoLink(String linkId, RepoDescriptor repoDescriptor) {
            super(linkId, "Delete");
            add(new CssClass("icon-link RemoveAction"));
            this.repoDescriptor = repoDescriptor;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            getDescriptorForEditing().removeRepository(repoDescriptor.getKey());
            centralConfigService.saveEditedDescriptorAndReload();
            // reload the whole page to refresh all components (and to use the new descriptors)
            ((RepositoryConfigPage) getPage()).refresh(target);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new IAjaxCallDecorator() {
                public CharSequence decorateScript(CharSequence script) {
                    return "if (confirm('Are you sure you wish to delete the repository "
                            + repoDescriptor.getKey() + "?')) {"
                            + script + "} else { return false; }";
                }

                public CharSequence decorateOnSuccessScript(CharSequence script) {
                    return script;
                }

                public CharSequence decorateOnFailureScript(CharSequence script) {
                    return script;
                }
            };
        }
    }

    private abstract static class EditLink extends ModalShowLink {
        private EditLink(String linkId) {
            super(linkId, "Edit");
            add(new CssClass("icon-link UpdateAction"));
        }
    }

    private abstract class RepoListPanel<T extends RepoDescriptor> extends OrderedListPanel<T> {
        protected RepoListPanel(String id, IModel listModel) {
            super(id, listModel);
        }

        protected abstract BaseModalPanel newUpdateItemPanel(T itemObject);

        @Override
        protected void populateItem(final ListItem item) {
            super.populateItem(item);
            item.add(new AjaxEventBehavior("oncontextmenu") {
                @SuppressWarnings({"unchecked"})
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    ModalHandler modalHandler = ModalHandler.getInstanceFor(RepoListPanel.this);
                    T itemObject = (T) item.getModelObject();
                    modalHandler.setModalPanel(newUpdateItemPanel(itemObject));
                    modalHandler.show(target);
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new CancelDefaultDecorator();
                }
            });
        }

        @Override
        protected List<? extends AbstractLink> getItemActions(final T itemObject, String linkId) {
            List<AbstractLink> links = new ArrayList<AbstractLink>();
            links.add(new EditLink(linkId) {
                @Override
                protected BaseModalPanel getModelPanel() {
                    return newUpdateItemPanel(itemObject);
                }
            });
            links.add(new DeleteRepoLink(linkId, itemObject));
            return links;
        }

        @Override
        protected void onOrderChanged(AjaxRequestTarget target) {
            super.onOrderChanged(target);
            ((RepositoryConfigPage) getPage()).refresh(target);
        }
    }

    private abstract class RepoListModel<T extends RepoDescriptor> implements IModel {
        public ArrayList<T> getObject() {
            return new ArrayList<T>(getRepos());
        }

        @SuppressWarnings({"unchecked"})
        public void setObject(Object object) {
            saveRepos((List<T>) object);
        }

        public void detach() {
        }

        protected abstract Collection<T> getRepos();

        protected abstract void saveRepos(List<T> repos);

        protected void assertLegalReorderedList(List<? extends RepoDescriptor> newList,
                                                Collection<? extends RepoDescriptor> original) {
            log.debug("Original ordered list: {}", original);
            log.debug("New ordered list: {}", newList);
            // make sure that the new reordered list contains the same repositories count and the
            // same repository keys
            if (newList.size() != original.size()) {
                throw new IllegalArgumentException(
                        "Invalid reordered repositories list: size doesn't match. " +
                                "Expected " + original.size() +
                                " but received " + newList.size());
            }
            if (!original.containsAll(newList)) {
                throw new IllegalArgumentException("Invalid reordered repositories list: " +
                        "the new list contains repository not in the original list");
            }
        }
    }
}
