/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.security.acl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.basic.SmartLinkLabel;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.checkbox.AjaxCheckboxColumn;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.GroupInfo;
import org.artifactory.security.MutableAclInfo;
import org.artifactory.security.MutablePermissionTargetInfo;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.util.AlreadyExistsException;
import org.artifactory.webapp.wicket.page.config.security.general.SecurityGeneralConfigPage;
import org.artifactory.webapp.wicket.page.security.acl.tabs.PermissionPanel;
import org.artifactory.webapp.wicket.page.security.acl.tabs.RepositoriesTabPanel;
import org.artifactory.webapp.wicket.panel.tabbed.StyledTabbedPanel;
import org.artifactory.webapp.wicket.panel.tabbed.SubmittingTabbedPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Yoav Landman
 * @author Yoav Aharoni
 */
public class PermissionTargetCreateUpdatePanel extends CreateUpdatePanel<MutablePermissionTargetInfo> {
    private static final Logger log = LoggerFactory.getLogger(PermissionTargetCreateUpdatePanel.class);

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private AclService aclService;

    @SpringBean
    private AuthorizationService authService;

    private PermissionTargetInfo permissionTarget;
    private RepoKeysData repoKeysData;
    private MutableAclInfo aclInfo;
    private PermissionTargetAceInfoRowDataProvider groupsDataProvider;
    private PermissionTargetAceInfoRowDataProvider usersDataProvider;

    public PermissionTargetCreateUpdatePanel(CreateUpdateAction action, MutablePermissionTargetInfo target,
            final Component targetsTable) {
        super(action, target);
        add(new CssClass("permissions-panel"));
        setWidth(600);
        setInitialHeight(740);
        setMinimalHeight(400);
        repoKeysData = new RepoKeysData(target);

        form.setOutputMarkupId(true);
        add(form);

        TitledBorder border = new TitledBorder("border");
        form.add(border);

        permissionTarget = target;

        if (isCreate()) {
            aclInfo = InfoFactoryHolder.get().createAcl(permissionTarget);
        } else {
            aclInfo = InfoFactoryHolder.get().copyAcl(aclService.getAcl(permissionTarget));
        }

        groupsDataProvider = new PermissionTargetAceInfoRowDataProvider(userGroupService, aclInfo) {
            @Override
            protected List<UserInfo> getUsers() {
                return Collections.emptyList();
            }
        };
        usersDataProvider = new PermissionTargetAceInfoRowDataProvider(userGroupService, aclInfo) {
            @Override
            protected List<GroupInfo> getGroups() {
                return Collections.emptyList();
            }
        };

        addTabs(border);
        addPermissionTargetNameField(border);

        addCancelButton();
        addSubmitButton(targetsTable);
    }

    @Override
    public void onShow(AjaxRequestTarget target) {
        super.onShow(target);
        target.appendJavaScript("PermissionTabPanel.onShow()");
    }

    private void addPermissionTargetNameField(TitledBorder border) {
        TextField nameTf = new TextField("name");
        setDefaultFocusField(nameTf);
        border.add(nameTf);
        if (!isSystemAdmin() || !isCreate()) {
            nameTf.setEnabled(false);
        }
    }

    /**
     * @return True if the current user is a system admin (not just the current permission target admin). Non system
     *         admins can only change the recipients table.
     */
    public boolean isSystemAdmin() {
        return authService.isAdmin();
    }

    public RepoKeysData getRepoKeysData() {
        return repoKeysData;
    }

    private void addCancelButton() {
        TitledAjaxLink cancel = new TitledAjaxLink("cancel", "Cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                cancel();
                ModalHandler.closeCurrent(target);
            }
        };
        form.add(cancel);
    }

    private void addSubmitButton(final Component targetsTable) {
        String submitCaption = isCreate() ? "Create" : "Save";
        TitledAjaxSubmitLink submit = new TitledAjaxSubmitLink("submit", submitCaption, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final String name = entity.getName();
                if (StringUtils.isBlank(name)) {
                    error("Field 'Name' is required.");
                    AjaxUtils.refreshFeedback(target);
                    return;
                }

                boolean anySelected = repoKeysData.isAnyCachedRepository() && repoKeysData.isAnyLocalRepository();
                repoKeysData.setAnyRepository(anySelected);

                entity.setRepoKeys(repoKeysData.getSelectedKeysList());
                aclInfo.setPermissionTarget(entity);
                if (isCreate()) {
                    try {
                        aclService.createAcl(aclInfo);
                        AccessLogger.created("Permission target " + name + " was created successfully");
                    } catch (Exception e) {
                        String msg;
                        if (e instanceof AlreadyExistsException) {
                            msg = "Permission target '" + name + "' already exists.";
                        } else {
                            msg = "Failed to create permissions target: " + e.getMessage();
                            log.error(msg, e);
                        }
                        getPage().error(msg);
                        AjaxUtils.refreshFeedback(target);
                        return;
                    }
                    getPage().info("Permission target '" + name + "' created successfully.");
                } else {
                    try {
                        aclService.updateAcl(aclInfo);
                        reloadData();
                        String message = "Permission target '" + name + "' updated successfully.";
                        AccessLogger.updated(message);
                        getPage().info(message);
                        target.add(PermissionTargetCreateUpdatePanel.this);
                    } catch (Exception e) {
                        String msg = "Failed to update permissions target: " + e.getMessage();
                        log.error(msg, e);
                        getPage().error(msg);
                        AjaxUtils.refreshFeedback(target);
                        return;
                    }
                }
                //Close the modal window and re-render the table
                targetsTable.modelChanged();
                target.add(targetsTable);
                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }
        };
        form.add(submit);
        form.add(new DefaultButtonBehavior(submit));
    }

    public List<LocalRepoDescriptor> getLocalRepositoryDescriptors() {
        return repositoryService.getLocalRepoDescriptors();
    }

    public List<? extends LocalRepoDescriptor> getCachedRepositoryDescriptors() {
        return repositoryService.getCachedRepoDescriptors();
    }

    void enableFields() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public String getCookieName() {
        return null;
    }

    private void addTabs(TitledBorder border) {
        List<ITab> tabs = new ArrayList<ITab>(2);

        tabs.add(new AbstractTab(Model.of("Repositories")) {
            @Override
            public Panel getPanel(String panelId) {
                return new RepositoriesTabPanel(panelId, PermissionTargetCreateUpdatePanel.this);
            }
        });

        tabs.add(new AbstractTab(Model.of("Users")) {
            @Override
            public Panel getPanel(String panelId) {
                return new PermissionPanel(PermissionTargetCreateUpdatePanel.this, panelId, false);
            }
        });

        if (!groupsDataProvider.getGroups().isEmpty()) {
            tabs.add(new AbstractTab(Model.of("Groups")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new PermissionPanel(PermissionTargetCreateUpdatePanel.this, panelId, true);
                }
            });
        }

        StyledTabbedPanel permissionsTabs = new SubmittingTabbedPanel("permissionsTabs", tabs);

        border.add(permissionsTabs);
    }

    public SortableTable getPermissionsTable(final boolean isGroup) {
        List<IColumn<AceInfoRow>> columns = Lists.newArrayList();
        columns.add(new PropertyColumn<AceInfoRow>(Model.of("Principal"), "principal", "principal") {
            @Override
            public void populateItem(Item<ICellPopulator<AceInfoRow>> item, String componentId,
                    IModel<AceInfoRow> model) {

                //If the item is an anonymous user and the access is disabled, warn
                String username = model.getObject().getPrincipal();
                if (UserInfo.ANONYMOUS.equals(username) && !authService.isAnonAccessEnabled()) {
                    CharSequence pageUrl = urlFor(SecurityGeneralConfigPage.class, new PageParameters());

                    StringBuilder usernameLabelBuilder = new StringBuilder(username).append(" (");
                    if (authService.isAdmin()) {
                        usernameLabelBuilder.append("<a href=\"").append(pageUrl).append("\">");
                    }
                    usernameLabelBuilder.append("disabled");
                    if (authService.isAdmin()) {
                        usernameLabelBuilder.append("</a>");
                    }
                    usernameLabelBuilder.append(")");
                    item.add(new SmartLinkLabel(componentId, usernameLabelBuilder.toString()).
                            setEscapeModelStrings(false));
                } else {
                    super.populateItem(item, componentId, model);
                }

                if (isGroup) {
                    item.add(new CssClass("group"));
                } else {
                    item.add(new CssClass("user"));
                }
            }
        });
        columns.add(new RoleCheckboxColumn("Manage", "manage") {
            @Override
            protected void onUpdate(FormComponent checkbox, AceInfoRow row, boolean value, AjaxRequestTarget target) {
                super.onUpdate(checkbox, row, value, target);
                if (sanityCheckAdmin() && isEnabled(row)) {
                    row.setManage(value);
                    onCheckboxUpdate(checkbox, target);
                }
            }

            @Override
            protected boolean isEnabled(AceInfoRow row) {
                if (!super.isEnabled(row)) {
                    return false;
                }

                String currentUsername = authService.currentUsername();
                String username = row.getPrincipal();
                //Do not allow admin user to change (revoke) his admin bit
                return !username.equals(currentUsername);

            }
        });
        columns.add(new RoleCheckboxColumn("Delete", "delete") {
            @Override
            protected void onUpdate(FormComponent checkbox, AceInfoRow row, boolean value, AjaxRequestTarget target) {
                super.onUpdate(checkbox, row, value, target);
                if (sanityCheckAdmin()) {
                    row.setDelete(value);
                    AccessLogger.deleted("Permission target" + row.getPrincipal() + " was successfully deleted");
                    onCheckboxUpdate(checkbox, target);
                }
            }
        });
        columns.add(new RoleCheckboxColumn("Deploy", "deploy") {
            @Override
            protected void onUpdate(FormComponent checkbox, AceInfoRow row, boolean value, AjaxRequestTarget target) {
                super.onUpdate(checkbox, row, value, target);
                if (sanityCheckAdmin()) {
                    row.setDeploy(value);
                    onCheckboxUpdate(checkbox, target);
                }
            }
        });
        columns.add(new RoleCheckboxColumn("Annotate", "annotate") {
            @Override
            protected void onUpdate(FormComponent checkbox, AceInfoRow row, boolean value, AjaxRequestTarget target) {
                super.onUpdate(checkbox, row, value, target);
                if (sanityCheckAdmin()) {
                    row.setAnnotate(value);
                    onCheckboxUpdate(checkbox, target);
                }
            }
        });
        columns.add(new RoleCheckboxColumn("Read", "read") {
            @Override
            protected void onUpdate(FormComponent checkbox, AceInfoRow row, boolean value, AjaxRequestTarget target) {
                super.onUpdate(checkbox, row, value, target);
                if (sanityCheckAdmin()) {
                    row.setRead(value);
                    onCheckboxUpdate(checkbox, target);
                }
            }
        });

        PermissionTargetAceInfoRowDataProvider dataProvider = isGroup ? groupsDataProvider : usersDataProvider;

        SortableTable table = new SortableTable<AceInfoRow>("recipients", columns, dataProvider, 15);
        //Recipients header
        Label recipientsHeader = new Label("recipientsHeader");
        recipientsHeader.setDefaultModel(
                Model.of("Permissions for \"" + permissionTarget.getName() + "\""));

        return table;
    }

    private void onCheckboxUpdate(FormComponent checkbox, AjaxRequestTarget target) {
        final MarkupContainer row = checkbox.findParent(OddEvenItem.class);
        target.addChildren(row, StyledCheckbox.class);
    }

    private void reloadData() {
        //Reload from backend
        aclInfo = InfoFactoryHolder.get().copyAcl(aclService.getAcl(permissionTarget));
        groupsDataProvider.setAclInfo(aclInfo);
        groupsDataProvider.loadData();

        usersDataProvider.setAclInfo(aclInfo);
        usersDataProvider.loadData();
    }

    public void cancel() {
        if (!isCreate()) {
            reloadData();
        }
    }

    public MutableAclInfo getAclInfo() {
        return aclInfo;
    }

    private boolean sanityCheckAdmin() {
        if (!isCreate() && !aclService.canManage(permissionTarget)) {
            String username = authService.currentUsername();
            log.error(username + " operation ignored: not enough permissions to administer '" +
                    permissionTarget + "'.");
            return false;
        }
        return true;
    }

    private class RoleCheckboxColumn extends AjaxCheckboxColumn<AceInfoRow> {
        private RoleCheckboxColumn(String title, String expression) {
            super(title, expression, expression);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new NoAjaxIndicatorDecorator();
        }

        @Override
        protected FormComponent<Boolean> newCheckBox(String id, IModel<Boolean> model, AceInfoRow rowObject) {
            FormComponent<Boolean> component = super.newCheckBox(id, model, rowObject);
            component.setOutputMarkupId(true);
            return component;
        }

        @Override
        protected boolean isEnabled(AceInfoRow row) {
            return true;
        }
    }

    public class RepoKeysData implements Serializable {
        private boolean anyRepository;
        private boolean anyLocalRepository;
        private boolean anyCachedRepository;
        private final Boolean CACHE_REPO = Boolean.TRUE;
        private Multimap<Boolean, LocalRepoDescriptor> repoKeyMap = HashMultimap.create();

        private RepoKeysData(PermissionTargetInfo info) {
            List<String> repoKeys = info.getRepoKeys();
            List<LocalRepoDescriptor> repoDescriptorList = getRepoDescriptors(repoKeys);
            anyRepository = repoKeys.contains(PermissionTargetInfo.ANY_REPO);
            if (anyRepository) {
                anyLocalRepository = true;
                anyCachedRepository = true;
            } else {
                anyLocalRepository = repoKeys.contains(PermissionTargetInfo.ANY_LOCAL_REPO);
                anyCachedRepository = repoKeys.contains(PermissionTargetInfo.ANY_REMOTE_REPO);
            }

            if (anyLocalRepository) {
                // add all local to selection
                repoKeyMap.putAll(!CACHE_REPO, getLocalRepositoryDescriptors());
            }

            if (anyCachedRepository) {
                // add all remote caches to selection
                repoKeyMap.putAll(CACHE_REPO, getCachedRepositoryDescriptors());
            }

            if (!anyRepository) {
                for (LocalRepoDescriptor repoDescriptor : repoDescriptorList) {
                    if (repoDescriptor.isCache()) {
                        repoKeyMap.put(CACHE_REPO, repoDescriptor);
                    } else {
                        repoKeyMap.put(!CACHE_REPO, repoDescriptor);
                    }
                }
            }
        }

        public List<String> getSelectedKeysList() {
            List<String> repoKeys = new ArrayList<String>();
            if (anyRepository) {
                repoKeys.add(PermissionTargetInfo.ANY_REPO);
            } else {
                if (anyLocalRepository) {
                    repoKeys.add(PermissionTargetInfo.ANY_LOCAL_REPO);
                } else {
                    Collection<LocalRepoDescriptor> collection = repoKeyMap.get(!CACHE_REPO);
                    if (collection != null) {
                        for (LocalRepoDescriptor repoDescriptor : collection) {
                            repoKeys.add(repoDescriptor.getKey());
                        }
                    }
                }
                if (anyCachedRepository) {
                    repoKeys.add(PermissionTargetInfo.ANY_REMOTE_REPO);
                } else {
                    Collection<LocalRepoDescriptor> collection = repoKeyMap.get(CACHE_REPO);
                    if (collection != null) {
                        for (LocalRepoDescriptor localRepoDescriptor : collection) {
                            repoKeys.add(localRepoDescriptor.getKey());
                        }
                    }
                }
            }
            return repoKeys;
        }

        public List<LocalRepoDescriptor> getRepoDescriptors() {
            return new ArrayList<LocalRepoDescriptor>(repoKeyMap.values());
        }

        public void setRepoDescriptors(List<? extends LocalRepoDescriptor> repoDescriptors) {
            repoKeyMap.clear();
            addRepoDescriptors(repoDescriptors);
        }

        public void addRepoDescriptors(List<? extends LocalRepoDescriptor> repoDescriptors) {
            for (LocalRepoDescriptor repoDescriptor : repoDescriptors) {
                if (!repoKeyMap.containsValue(repoDescriptor)) {
                    repoKeyMap.put(repoDescriptor.isCache(), repoDescriptor);
                }
            }
        }

        public void removeRepoDescriptors(List<? extends LocalRepoDescriptor> repoDescriptors) {
            for (LocalRepoDescriptor repoDescriptor : repoDescriptors) {
                if (repoKeyMap.containsValue(repoDescriptor)) {
                    repoKeyMap.remove(repoDescriptor.isCache(), repoDescriptor);
                }
            }
        }

        public boolean isAnyRepository() {
            return anyRepository;
        }

        public void setAnyRepository(boolean anyRepository) {
            this.anyRepository = anyRepository;
        }

        public boolean isAnyLocalRepository() {
            return anyLocalRepository;
        }

        public void setAnyLocalRepository(boolean anyLocalRepository) {
            this.anyLocalRepository = anyLocalRepository;
        }

        public boolean isAnyCachedRepository() {
            return anyCachedRepository;
        }

        public void setAnyCachedRepository(boolean anyCachedRepository) {
            this.anyCachedRepository = anyCachedRepository;
        }

        private List<LocalRepoDescriptor> getRepoDescriptors(List<String> repoKeys) {
            List<LocalRepoDescriptor> descriptorList = new ArrayList<LocalRepoDescriptor>();
            for (String repoKey : repoKeys) {
                LocalRepoDescriptor repoDescriptor = repositoryService.localOrCachedRepoDescriptorByKey(repoKey);
                if (repoDescriptor != null) {
                    descriptorList.add(repoDescriptor);
                }
            }
            return descriptorList;
        }
    }
}
