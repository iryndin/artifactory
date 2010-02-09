package org.artifactory.webapp.wicket.page.security.user;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.common.component.BooleanColumn;
import org.artifactory.webapp.wicket.common.component.CheckboxColumn;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.list.ListPanel;
import org.artifactory.webapp.wicket.page.security.user.column.UserColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The users table is special in that it is sensitive to a filter and also contains
 * a checkbox column.
 *
 * @author Yossi Shaul
 */
public class UsersTable extends ListPanel<UserModel> {
    @SpringBean
    private UserGroupService userGroupService;

    private UsersTableDataProvider dataProvider;

    public UsersTable(String id, UsersTableDataProvider dataProvider) {
        super(id, dataProvider);
        this.dataProvider = dataProvider;
    }

    @Override
    public String getTitle() {
        return "Users List";
    }

    @Override
    protected List<UserModel> getList() {
        throw new UnsupportedOperationException("Users table uses it's own data provider");
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new CheckboxColumn<UserModel>("", "selected", this) {
            @Override
            protected void doUpdate(UserModel userModel, boolean checked,
                                    AjaxRequestTarget target) {
                // nothing special
            }
        });

        columns.add(new UserColumn(new Model("User name")));
        columns.add(new BooleanColumn(new Model("Admin"), "adminString", "admin"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        Set<String> defaultGroupsNames = userGroupService.getNewUserDefaultGroupsNames();
        return new UserCreateUpdatePanel(CreateUpdateAction.CREATE, new UserModel(defaultGroupsNames), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(UserModel user) {
        return new UserCreateUpdatePanel(CreateUpdateAction.UPDATE, user, this);
    }

    @Override
    protected String getDeleteConfirmationText(UserModel user) {
        return "Are you sure you wish to delete the user " + user.getUsername() + "?";
    }

    @Override
    protected void deleteItem(UserModel user, AjaxRequestTarget target) {
        userGroupService.deleteUser(user.getUsername());
        refreshUsersList(target);
    }

    public void refreshUsersList(AjaxRequestTarget target) {
        dataProvider.recalcUsersList();
        target.addComponent(this);
    }

    List<String> getSelectedUsernames() {
        List<String> selectedUsernames = new ArrayList<String>();
        for (UserModel userModel : dataProvider.getUsers()) {
            if (userModel.isSelected()) {
                selectedUsernames.add(userModel.getUsername());
            }
        }
        return selectedUsernames;
    }

    // TODO: handle on row selected
    /*
        @Override
        protected void onRowSelected(UserModel selection, AjaxRequestTarget target) {
            super.onRowSelected(selection, target);
            for (UserModel user : users) {
                user.setSelected(false);
            }
            selection.setSelected(true);

            ModalHandler modal = ModalHandler.getInstanceFor(this);
            modal.setModalPanel(getUsersPage().newUpdatePanel(selection));
            modal.show(target);
        }
    */
}
