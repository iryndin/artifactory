/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.security.acls;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.security.ExtendedAclService;
import org.artifactory.security.RepoPathAce;
import org.artifactory.security.RepoPathAcl;
import org.artifactory.security.SecuredRepoPath;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.CheckboxColumn;
import org.artifactory.webapp.wicket.components.panel.FeedbackEnabledPanel;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class PermissionTargetRecipientsPanel extends FeedbackEnabledPanel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(PermissionTargetRecipientsPanel.class);

    private Set<Row> updatedRows = new HashSet<Row>();

    public PermissionTargetRecipientsPanel(String id) {
        super(id);
        setOutputMarkupId(true);
        //Add a dummy div so that it can be ajax'ed replaced
        WebMarkupContainer tableContainer = new WebMarkupContainer("recipientsContainer");
        add(tableContainer);
    }

    //A dropdown for the users - the ones already chosen + add/remove button
    public void updateModelObject(
            final SecuredRepoPath permissionTarget, final WebMarkupContainer container,
            AjaxRequestTarget target) {
        setVisible(true);
        //Permissions table
        final SortableAclsDataProvider dataProvider =
                new SortableAclsDataProvider(permissionTarget);
        final WebMarkupContainer tableContainer = new WebMarkupContainer("recipientsContainer");
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(
                new Model("User"), "recipient.username", "recipient.username"));
        columns.add(new CheckboxColumn<Row>("Admin", "admin", "admin", tableContainer) {
            protected void doUpdate(Row row, boolean value, AjaxRequestTarget target) {
                if (sanityCheckAdmin(permissionTarget) && isEnabled(row)) {
                    row.setAdmin(value);
                    updatedRows.add(row);
                    clearFeedback(target);
                }
            }

            @Override
            protected boolean isEnabled(Row row) {
                String currentUsername = ArtifactorySecurityManager.getUsername();
                String username = row.getRecipient().getUsername();
                //Do not allow admin user to change (revoke) his admin bit
                return !username.equals(currentUsername);
            }
        });
        columns.add(new CheckboxColumn<Row>("Deployer", "deployer", "deployer", tableContainer) {
            protected void doUpdate(Row row, boolean value, AjaxRequestTarget target) {
                if (sanityCheckAdmin(permissionTarget)) {
                    row.setDeployer(value);
                    updatedRows.add(row);
                    clearFeedback(target);
                }
            }
        });
        columns.add(new CheckboxColumn<Row>("Reader", "reader", "reader", tableContainer) {
            protected void doUpdate(Row row, boolean value, AjaxRequestTarget target) {
                if (sanityCheckAdmin(permissionTarget)) {
                    row.setReader(value);
                    updatedRows.add(row);
                    clearFeedback(target);
                }
            }
        });
        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("recipients", columns, dataProvider, 5);
        //Recipients header
        Label recipientsHeader = new Label("recipientsHeader");
        recipientsHeader.setModel(
                new Model("\"" + permissionTarget.getIdentifier() + "\" Permissions"));

        tableContainer.add(new AjaxFallbackLink("cancel") {
            public void onClick(AjaxRequestTarget target) {
                updatedRows.clear();
                dataProvider.loadData();
                target.addComponent(tableContainer);
                clearFeedback(target);
            }
        });
        tableContainer.add(new AjaxFallbackLink("submit") {
            public void onClick(AjaxRequestTarget target) {
                for (Row row : updatedRows) {
                    updateRow(row);
                }
                updatedRows.clear();
                dataProvider.loadData();
                target.addComponent(tableContainer);
                info("Permissions updated successfully.");
                target.addComponent(getFeedback());
            }
        });
        tableContainer.add(table);
        tableContainer.setOutputMarkupId(true);
        tableContainer.add(recipientsHeader);
        replace(tableContainer);
        target.addComponent(container);
    }

    private static boolean sanityCheckAdmin(SecuredRepoPath repoPath) {
        ArtifactorySecurityManager security = ContextHelper.get().getSecurity();
        if (!security.canAdmin(repoPath)) {
            String username = ArtifactorySecurityManager.getUsername();
            LOGGER.error(username +
                    " operation ignored: not enough permissions to administer '" +
                    repoPath + "'.");
            return false;
        }
        return true;
    }

    private static void updateRow(Row row) {
        ArtifactoryContext context = ContextHelper.get();
        ArtifactorySecurityManager security = context.getSecurity();
        ExtendedAclService aclService = security.getAclService();
        SecuredRepoPath repoPath = row.getRepoPath();
        RepoPathAcl acl = aclService.readAclById(repoPath);
        RepoPathAce ace = row.toAclEntry();
        acl.updateOrCreateAce(ace);
        aclService.updateAcl(acl);
    }

    private static class Row implements Serializable {
        private SecuredRepoPath repoPath;
        private SimpleUser recipient;
        private boolean admin;
        private boolean deployer;
        private boolean reader;

        public Row(SimpleUser recipient, SecuredRepoPath repoPath, int mask) {
            this.recipient = recipient;
            this.repoPath = repoPath;
            admin = (mask & BasePermission.ADMINISTRATION.getMask()) > 0;
            deployer = (mask & BasePermission.WRITE.getMask()) > 0;
            reader = (mask & BasePermission.READ.getMask()) > 0;
        }

        public SecuredRepoPath getRepoPath() {
            return repoPath;
        }

        public UserDetails getRecipient() {
            return recipient;
        }

        public boolean getAdmin() {
            return admin;
        }

        public boolean isAdmin() {
            return admin;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
            if (admin) {
                setDeployer(true);
            }
        }

        public boolean isDeployer() {
            return deployer;
        }

        public void setDeployer(boolean deployer) {
            this.deployer = deployer;
            if (deployer) {
                setReader(true);
            }
        }

        public boolean isReader() {
            return reader;
        }

        public void setReader(boolean reader) {
            this.reader = reader;
        }

        public RepoPathAce toAclEntry() {
            CumulativePermission permission = new CumulativePermission();
            if (admin) {
                permission.set(BasePermission.ADMINISTRATION);
            }
            if (deployer) {
                permission.set(BasePermission.WRITE);
            }
            if (reader) {
                permission.set(BasePermission.READ);
            }
            RepoPathAcl acl = new RepoPathAcl(repoPath);
            return new RepoPathAce(acl, permission, recipient.toPrincipalSid());
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Row)) {
                return false;
            }
            Row row = (Row) o;
            return !(repoPath != null ? !repoPath.equals(row.repoPath) :
                    row.repoPath != null) &&
                    !(recipient != null ? !recipient.equals(row.recipient) : row.recipient != null);
        }

        public int hashCode() {
            int result;
            result = (repoPath != null ? repoPath.hashCode() : 0);
            result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
            result = 31 * result + (admin ? 1 : 0);
            result = 31 * result + (deployer ? 1 : 0);
            result = 31 * result + (reader ? 1 : 0);
            return result;
        }
    }

    private static class SortableAclsDataProvider extends SortableDataProvider {

        private SecuredRepoPath repoPath;
        private List<Row> acls;

        public SortableAclsDataProvider(SecuredRepoPath repoPath) {
            this.repoPath = repoPath;
            //Set default sort
            setSort("recipient.username", true);
            //Load the acls
            loadData();
        }

        public Iterator iterator(int first, int count) {
            SortParam sp = getSort();
            String sortProp = sp.getProperty();
            boolean asc = sp.isAscending();
            if ("recipient.username".equals(sortProp)) {
                if (asc) {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            String o1 = r1.getRecipient().getUsername();
                            String o2 = r2.getRecipient().getUsername();
                            return o1.compareTo(o2);
                        }
                    });
                } else {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            String o1 = r1.getRecipient().getUsername();
                            String o2 = r2.getRecipient().getUsername();
                            return o2.compareTo(o1);
                        }
                    });
                }
            } else if ("admin".equals(sortProp)) {
                if (asc) {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            boolean o1 = r1.isAdmin();
                            boolean o2 = r2.isAdmin();
                            if (o1 && o2) {
                                return 0;
                            }
                            if (!o1 && !o2) {
                                return 0;
                            }
                            if (o1 && !o2) {
                                return 1;
                            }
                            return -1;
                        }
                    });
                } else {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            boolean o1 = r1.isAdmin();
                            boolean o2 = r2.isAdmin();
                            if (o1 && o2) {
                                return 0;
                            }
                            if (!o1 && !o2) {
                                return 0;
                            }
                            if (o2 && !o1) {
                                return 1;
                            }
                            return -1;
                        }
                    });
                }
            } else if ("deployer".equals(sortProp)) {
                if (asc) {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            boolean o1 = r1.isDeployer();
                            boolean o2 = r2.isDeployer();
                            if (o1 && o2) {
                                return 0;
                            }
                            if (!o1 && !o2) {
                                return 0;
                            }
                            if (o1 && !o2) {
                                return 1;
                            }
                            return -1;
                        }
                    });
                } else {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            boolean o1 = r1.isDeployer();
                            boolean o2 = r2.isDeployer();
                            if (o1 && o2) {
                                return 0;
                            }
                            if (!o1 && !o2) {
                                return 0;
                            }
                            if (o2 && !o1) {
                                return 1;
                            }
                            return -1;
                        }
                    });
                }
            } else if ("reader".equals(sortProp)) {
                if (asc) {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            boolean o1 = r1.isReader();
                            boolean o2 = r2.isReader();
                            if (o1 && o2) {
                                return 0;
                            }
                            if (!o1 && !o2) {
                                return 0;
                            }
                            if (o1 && !o2) {
                                return 1;
                            }
                            return -1;
                        }
                    });
                } else {
                    Collections.sort(acls, new Comparator<Row>() {
                        public int compare(Row r1, Row r2) {
                            boolean o1 = r1.isReader();
                            boolean o2 = r2.isReader();
                            if (o1 && o2) {
                                return 0;
                            }
                            if (!o1 && !o2) {
                                return 0;
                            }
                            if (o2 && !o1) {
                                return 1;
                            }
                            return -1;
                        }
                    });
                }
            }
            List<Row> list = acls.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return acls.size();
        }

        public IModel model(Object object) {
            return new Model((Row) object);
        }

        @SuppressWarnings({"UnnecessaryLocalVariable"})
        private void loadData() {
            ArtifactoryContext context = ContextHelper.get();
            ArtifactorySecurityManager security = context.getSecurity();
            //Restore the roles
            ExtendedAclService aclService = security.getAclService();
            RepoPathAcl acl = aclService.readAclById(repoPath);
            List<RepoPathAce> aces = acl.getAces();
            Set<UserDetails> admins = new HashSet<UserDetails>();
            Set<UserDetails> deployers = new HashSet<UserDetails>();
            Set<UserDetails> readers = new HashSet<UserDetails>();
            for (RepoPathAce ace : aces) {
                int mask = ace.getMask();
                String principal = ace.getPrincipal();
                User user = new SimpleUser(principal);
                if ((mask & BasePermission.ADMINISTRATION.getMask()) > 0) {
                    admins.add(user);
                }
                if ((mask & BasePermission.WRITE.getMask()) > 0) {
                    deployers.add(user);
                }
                if ((mask & BasePermission.READ.getMask()) > 0) {
                    readers.add(user);
                }
            }

            //List of recipients except for admins that are filtered out from acl management
            final List<SimpleUser> users = security.getUserDetailsService().getAllUsers(false);
            //Create a list of acls for *all* users (stored acls may not contain every user)
            List<Row> tableEntries = new ArrayList<Row>(users.size());
            for (SimpleUser user : users) {
                int mask = 0;
                if (admins.contains(user)) {
                    mask |= BasePermission.ADMINISTRATION.getMask();
                }
                if (deployers.contains(user)) {
                    mask |= BasePermission.WRITE.getMask();
                }
                if (readers.contains(user)) {
                    mask |= BasePermission.READ.getMask();
                }
                Row row = new Row(user, repoPath, mask);
                tableEntries.add(row);
            }
            acls = tableEntries;
        }
    }
}
