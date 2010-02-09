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

import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.BasicAclEntry;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.security.ExtendedJdbcAclDao;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.CheckboxColumn;

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
public class PermissionTargetRecipientsPanel extends Panel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(PermissionTargetRecipientsPanel.class);

    public PermissionTargetRecipientsPanel(String id) {
        super(id);
        setOutputMarkupId(true);
        //Feedback
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        //Add a dummy div so that it can be ajax'ed replaced
        WebMarkupContainer tableContainer = new WebMarkupContainer("recipientsContainer");
        add(tableContainer);
    }

    //A dropdown for the users - the ones already chosen + add/remove button
    public void updateModelObject(
            final RepoPath permissionTarget, final WebMarkupContainer container,
            AjaxRequestTarget target) {
        setVisible(true);
        WebMarkupContainer tableContainer = new WebMarkupContainer("recipientsContainer");
        tableContainer.setOutputMarkupId(true);
        //Permissions table
        final SortableAclsDataProvider dataProvider =
                new SortableAclsDataProvider(permissionTarget);
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(
                new Model("User"), "recipient.username", "recipient.username"));
        columns.add(new CheckboxColumn<Row>("Admin", "admin", "admin", tableContainer) {
            protected void doUpdate(Row row, boolean value) {
                if (sanityCheckAdmin(permissionTarget) && isEnabled(row)) {
                    row.setAdmin(value);
                    updateRow(row);
                }
            }

            @Override
            protected boolean isEnabled(Row row) {
                String currentUsername = SecurityHelper.getUsername();
                String username = row.getRecipient().getUsername();
                //Do not allow admin user to change (revoke) his admin bit
                return !username.equals(currentUsername);
            }
        });
        columns.add(new CheckboxColumn<Row>("Deployer", "deployer", "deployer", tableContainer) {
            protected void doUpdate(Row row, boolean value) {
                if (sanityCheckAdmin(permissionTarget)) {
                    row.setDeployer(value);
                    updateRow(row);
                }
            }
        });
        columns.add(new CheckboxColumn<Row>("Reader", "reader", "reader", tableContainer) {
            protected void doUpdate(Row row, boolean value) {
                if (sanityCheckAdmin(permissionTarget)) {
                    row.setReader(value);
                    updateRow(row);
                }
            }
        });
        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("recipients", columns, dataProvider, 5);
        //Recipients header
        Label recipientsHeader = new Label("recipientsHeader");
        recipientsHeader.setModel(
                new Model("\"" + permissionTarget.getId() + "\" Permissions"));
        tableContainer.add(recipientsHeader);
        tableContainer.add(table);
        replace(tableContainer);
        target.addComponent(container);
    }

    private boolean sanityCheckAdmin(RepoPath repoPath) {
        SecurityHelper security = ContextHelper.get().getSecurity();
        if (!security.canAdmin(repoPath)) {
            String username = SecurityHelper.getUsername();
            LOGGER.error(username +
                    " operation ignored: not enough permissions to administer '" +
                    repoPath + "'.");
            return false;
        }
        return true;
    }

    private void updateRow(Row row) {
        ArtifactoryContext context = ContextHelper.get();
        SecurityHelper security = context.getSecurity();
        ExtendedJdbcAclDao aclDao = security.getAclDao();
        SimpleAclEntry aclEntry = row.asAclEntry();
        aclDao.update(aclEntry);
    }

    private class Row implements Serializable {
        private AclObjectIdentity permissionTarget;
        private SimpleUser recipient;
        private boolean admin;
        private boolean deployer;
        private boolean reader;

        public Row(SimpleUser recipient, RepoPath permissionTarget, int mask) {
            this.recipient = recipient;
            this.permissionTarget = permissionTarget;
            admin = (mask & SimpleAclEntry.ADMINISTRATION) > 0;
            deployer = (mask & SimpleAclEntry.WRITE) > 0;
            reader = (mask & SimpleAclEntry.READ) > 0;
        }

        public AclObjectIdentity getPermissionTarget() {
            return permissionTarget;
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

        public SimpleAclEntry asAclEntry() {
            int mask = 0;
            if (admin) {
                mask |= SimpleAclEntry.ADMINISTRATION;
            }
            if (deployer) {
                mask |= SimpleAclEntry.WRITE;
            }
            if (reader) {
                mask |= SimpleAclEntry.READ;
            }
            return new SimpleAclEntry(recipient, permissionTarget, null, mask);
        }
    }

    private class SortableAclsDataProvider extends SortableDataProvider {

        private RepoPath permissionTarget;
        private List<Row> acls;

        public SortableAclsDataProvider(RepoPath permissionTarget) {
            this.permissionTarget = permissionTarget;
            //Set default sort
            setSort("recipient.username", true);
            //Load the acls
            acls = loadData();
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
        private List<Row> loadData() {
            ArtifactoryContext context = ContextHelper.get();
            SecurityHelper security = context.getSecurity();
            //Restore the roles
            BasicAclEntry[] entries = security.getAclDao().getAcls(permissionTarget);
            Set<UserDetails> admins = new HashSet<UserDetails>();
            Set<UserDetails> deployers = new HashSet<UserDetails>();
            Set<UserDetails> readers = new HashSet<UserDetails>();
            for (BasicAclEntry entry : entries) {
                int mask = entry.getMask();
                String recipient = (String) entry.getRecipient();
                User user = new SimpleUser(recipient);
                if ((mask & SimpleAclEntry.ADMINISTRATION) > 0) {
                    admins.add(user);
                }
                if ((mask & SimpleAclEntry.WRITE) > 0) {
                    deployers.add(user);
                }
                if ((mask & SimpleAclEntry.READ) > 0) {
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
                    mask |= SimpleAclEntry.ADMINISTRATION;
                }
                if (deployers.contains(user)) {
                    mask |= SimpleAclEntry.WRITE;
                }
                if (readers.contains(user)) {
                    mask |= SimpleAclEntry.READ;
                }
                Row row = new Row(user, permissionTarget, mask);
                tableEntries.add(row);
            }
            return tableEntries;
        }
    }
}
