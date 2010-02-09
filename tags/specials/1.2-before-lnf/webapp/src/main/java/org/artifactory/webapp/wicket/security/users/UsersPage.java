package org.artifactory.webapp.wicket.security.users;

import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.artifactory.security.ExtendedUserDetailsService;
import org.artifactory.security.SecurityHelper;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.CreateUpdatePanel;
import wicket.ajax.AjaxEventBehavior;
import wicket.ajax.AjaxRequestTarget;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import wicket.extensions.markup.html.repeater.data.table.IColumn;
import wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import wicket.extensions.markup.html.repeater.util.SortParam;
import wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.repeater.Item;
import wicket.model.IModel;
import wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@AuthorizeInstantiation("ADMIN")
public class UsersPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(UsersPage.class);


    public UsersPage() {
        WebMarkupContainer actionContainer = new WebMarkupContainer("action");
        actionContainer.setOutputMarkupId(true);
        final UserPanel createPanel =
                new UserPanel("create", CreateUpdatePanel.CreateUpdateAction.CREATE, new User());
        final UserPanel updatePanel =
                new UserPanel("update", CreateUpdatePanel.CreateUpdateAction.UPDATE, new User());
        createPanel.setOtherPanel(updatePanel);
        updatePanel.setOtherPanel(createPanel);
        updatePanel.setVisible(false);
        actionContainer.add(createPanel);
        actionContainer.add(updatePanel);
        add(actionContainer);
        //Results table
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Username"), "username", "username"));
        columns.add(new PropertyColumn(new Model("Admin"), "admin", "admin"));
        columns.add(new AbstractColumn(new Model()) {
            public void populateItem(Item cellItem, String componentId, IModel model) {
                cellItem.add(new Delete(componentId, model, createPanel));
            }
        });
        AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("users", columns,
                new SortableUsersDataProvider(), 10) {

            //Handle row selection
            @Override
            protected Item newCellItem(final String id, int index, final IModel model) {
                Item item = super.newCellItem(id, index, model);
                Object modelObject = model.getObject(this);
                if (modelObject instanceof PropertyColumn) {
                    //Don't add behavior to the delete column
                    item.add(new AjaxEventBehavior("onMouseUp") {
                        protected void onEvent(final AjaxRequestTarget target) {
                            //Show the update panel
                            User user =
                                    (User) getComponent().getParent().getParent().getModelObject();
                            updatePanel.updateModelObject(user, target);
                        }
                    });
                }
                return item;
            }
        };
        add(table);
        //Update the results when a user is created/updated
        createPanel.setChangeListener(table);
        updatePanel.setChangeListener(table);
    }

    protected String getPageName() {
        return "Users Management";
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private List<User> getUsers() {
        SecurityHelper security = getContext().getSecurity();
        List<UserDetails> userDetails = security.getUserDetailsService().getAllUsers();
        List<User> users = new ArrayList<User>();
        for (UserDetails detail : userDetails) {
            users.add(new User(detail));
        }
        return users;
    }

    private class SortableUsersDataProvider extends SortableDataProvider {
        public SortableUsersDataProvider() {
            //Set default sort
            setSort("username", true);
        }

        public Iterator iterator(int first, int count) {
            List<User> list = getUsers().subList(first, first + count);
            SortParam sp = getSort();
            boolean asc = sp.isAscending();
            String prop = sp.getProperty();
            if (("username").equals(prop) && asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u1.getUsername().compareTo(u2.getUsername());
                    }
                });
            } else if (("username").equals(prop) && !asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u2.getUsername().compareTo(u1.getUsername());
                    }
                });
            } else if (("admin").equals(prop) && asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u1.isAdmin() && (u2.isAdmin()) ? 1 : 0;
                    }
                });
            } else if (("admin").equals(prop) && !asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u1.isAdmin() && (u2.isAdmin()) ? 0 : 1;
                    }
                });
            }
            return list.iterator();
        }

        public int size() {
            return getUsers().size();
        }

        public IModel model(Object object) {
            return new Model((User) object);
        }
    }

    private class Delete extends WebMarkupContainer {

        private User user;

        public Delete(String id, IModel model, final UserPanel panel) {
            super(id);
            user = (User) model.getObject(this);
            add(new AjaxEventBehavior("onClick") {
                protected void onEvent(final AjaxRequestTarget target) {
                    SecurityHelper security = getContext().getSecurity();
                    ExtendedUserDetailsService userDetailsService =
                            security.getUserDetailsService();
                    String username = user.getUsername();
                    userDetailsService.deleteUser(username);
                    panel.show(target);
                }

                @SuppressWarnings({"UnnecessaryLocalVariable"})
                @Override
                protected CharSequence getCallbackScript() {
                    CharSequence orig = super.getCallbackScript();
                    String callbackScript =
                            "if (confirm('Are you sure you wish to delete the user " +
                                    user.getUsername() + "?')) {" +
                                    orig + "} else { return false; }";
                    return callbackScript;
                }

            });
        }

        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            tag.setName("img");
            tag.put("src", "../images/delete.png");
            tag.put("alt", "Delete");
            tag.put("style", "cursor:pointer;");
        }

        @Override
        protected void onComponentTagBody(
                final MarkupStream markupStream, final ComponentTag openTag) {
            replaceComponentTagBody(markupStream, openTag, "");
        }
    }
}
