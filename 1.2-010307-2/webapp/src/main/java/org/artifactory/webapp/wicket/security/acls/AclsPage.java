package org.artifactory.webapp.wicket.security.acls;

import org.apache.log4j.Logger;
import org.artifactory.security.ExtendedJdbcAclDao;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.StringObjectIdentity;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.DojoButton;
import org.artifactory.webapp.wicket.widget.AjaxDeleteRow;
import org.springframework.dao.DataIntegrityViolationException;
import wicket.Component;
import wicket.ajax.AjaxEventBehavior;
import wicket.ajax.AjaxRequestTarget;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import wicket.extensions.markup.html.repeater.data.table.IColumn;
import wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import wicket.extensions.markup.html.repeater.util.SortParam;
import wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.repeater.Item;
import wicket.model.CompoundPropertyModel;
import wicket.model.IModel;
import wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@AuthorizeInstantiation("ADMIN")
public class AclsPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(AclsPage.class);

    public AclsPage() {
        final WebMarkupContainer groupRecipientsPanelContainer = new WebMarkupContainer("panel");
        groupRecipientsPanelContainer.setOutputMarkupId(true);
        final GroupRecipientsPanel groupRecipientsPanel = new GroupRecipientsPanel("recipients");
        groupRecipientsPanel.setVisible(false);
        groupRecipientsPanelContainer.add(groupRecipientsPanel);
        add(groupRecipientsPanelContainer);
        //Results table
        final SortableGroupsDataProvider dataProvider = new SortableGroupsDataProvider();
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Group"), "groupId", "groupId"));
        columns.add(new AbstractColumn(new Model()) {
            public void populateItem(final Item cellItem, String componentId, IModel model) {
                cellItem.add(new AjaxDeleteRow<Group>(componentId, model, groupRecipientsPanel) {

                    protected void doDelete() {
                        SecurityHelper security = getContext().getSecurity();
                        ExtendedJdbcAclDao aclDao = security.getAclDao();
                        Group group = getToBeDeletedObject();
                        aclDao.delete(new StringObjectIdentity(group.getGroupId()));
                    }

                    protected void onDeleted(AjaxRequestTarget target, Component listener) {
                        GroupRecipientsPanel panel = (GroupRecipientsPanel) listener;
                        if (panel.isVisible()) {
                            panel.setVisible(false);
                            target.addComponent(panel);
                        }
                        Component table = cellItem.getPage().get("groups");
                        dataProvider.calcGroups();
                        target.addComponent(table);
                    }

                    protected String getConfirmationQuestion() {
                        String groupId = getToBeDeletedObject().getGroupId();
                        return "Are you sure you wish to delete the group " + groupId + "?";
                    }
                });
            }
        });
        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("groups", columns,
                        dataProvider, 10) {
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
                                    Group group = (Group) getComponent().getParent().getParent()
                                            .getModelObject();
                                    groupRecipientsPanel.updateModelObject(
                                            group, groupRecipientsPanelContainer, target);
                                }
                            });
                        }
                        return item;
                    }

                    @Override
                    protected void onModelChanged() {
                        dataProvider.calcGroups();
                    }
                };
        add(table);
        //Add a feedback panel
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        //Add the add group function
        Form form = new Form("newGroupForm", newEmptyGroup());
        form.setOutputMarkupId(true);
        //GroupId
        final RequiredTextField groupIdTf = new RequiredTextField("groupId");
        form.add(groupIdTf);
        //Cancel
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                form.setModel(newEmptyGroup());
                target.addComponent(form);
                target.addComponent(feedback);
                groupRecipientsPanel.setVisible(false);
                target.addComponent(groupRecipientsPanel);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        DojoButton submit = new DojoButton("submit", form, "Create") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                ArtifactoryPage page = (ArtifactoryPage) getPage();
                SecurityHelper security = page.getContext().getSecurity();
                ExtendedJdbcAclDao aclDao = security.getAclDao();
                String group = (String) groupIdTf.getModelObject();
                //TODO: [by yl] Do we want to compute a parent group?
                try {
                    aclDao.createAclObjectIdentity(new StringObjectIdentity(group), null);
                } catch (DataIntegrityViolationException e) {
                    error("Group '" + group + "' already exists.");
                    target.addComponent(feedback);
                    return;
                } catch (Exception e) {
                    error("Failed to create group: " + e.getMessage());
                    target.addComponent(feedback);
                    return;
                }
                //Rerender the table
                table.modelChanged();
                target.addComponent(table);
                form.setModel(newEmptyGroup());
                target.addComponent(form);
                info("Group '" + group + "' created successfully.");
                groupRecipientsPanel.setVisible(false);
                target.addComponent(groupRecipientsPanel);
                target.addComponent(feedback);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);
        add(form);
    }

    protected String getPageName() {
        return "Users Management";
    }

    private static CompoundPropertyModel newEmptyGroup() {
        return new CompoundPropertyModel(new Model(new Group()));
    }

    private class SortableGroupsDataProvider extends SortableDataProvider {

        private List<Group> groups;

        public SortableGroupsDataProvider() {
            //Set default sort
            setSort("groupId", true);
            //Load the groups
            groups = calcGroups();
        }

        public Iterator iterator(int first, int count) {
            List<Group> list = groups.subList(first, first + count);
            SortParam sp = getSort();
            boolean asc = sp.isAscending();
            if (asc) {
                Collections.sort(list, new Comparator<Group>() {
                    public int compare(Group g1, Group g2) {
                        return g1.getGroupId().compareTo(g2.getGroupId());
                    }
                });
            } else {
                Collections.sort(list, new Comparator<Group>() {
                    public int compare(Group g1, Group g2) {
                        return g2.getGroupId().compareTo(g1.getGroupId());
                    }
                });
            }
            return list.iterator();
        }

        public int size() {
            return groups.size();
        }

        public IModel model(Object object) {
            return new Model((Group) object);
        }

        @SuppressWarnings({"UnnecessaryLocalVariable"})
        private List<Group> calcGroups() {
            SecurityHelper security = getContext().getSecurity();
            List<String> groupNames = security.getAclDao().getAllGroups();
            List<Group> groups = new ArrayList<Group>(groupNames.size());
            for (String groupName : groupNames) {
                groups.add(new Group(groupName));
            }
            this.groups = groups;
            return groups;
        }
    }
}