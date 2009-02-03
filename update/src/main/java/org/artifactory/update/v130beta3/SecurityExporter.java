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
package org.artifactory.update.v130beta3;

import org.apache.jackrabbit.util.Text;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.update.jcr.JcrPathUpdate;
import org.artifactory.update.jcr.JcrSessionProvider;
import org.artifactory.update.utils.UpdateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * Security exporter based on the jcr repository. Works from 130beta1 to 130beta2.
 *
 * @author freds
 * @date Aug 14, 2008
 */
public class SecurityExporter implements ImportableExportable {
    private static final Logger log =
            LoggerFactory.getLogger(SecurityExporter.class);

    private static final String ACLS_KEY = "acls";
    private static final String OCM_CLASSNAME = "ocm:classname";
    private static final String USERS_KEY = "users";
    private static final String GROUPS_KEY = "groups";

    public static String getUsersJcrPath() {
        return JcrPathUpdate.getOcmClassJcrPath(USERS_KEY);
    }

    public static String getAclsJcrPath() {
        return JcrPathUpdate.getOcmClassJcrPath(ACLS_KEY);
    }

    public static String getGroupsJcrPath() {
        return JcrPathUpdate.getOcmClassJcrPath(GROUPS_KEY);
    }

    private JcrSessionProvider jcr;

    public JcrSessionProvider getJcr() {
        return jcr;
    }

    public void setJcr(JcrSessionProvider jcr) {
        this.jcr = jcr;
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Extracting all groups", log);
        List<GroupInfo> groups = getAllGroups();
        status.setStatus("Extracting all users", log);
        List<UserInfo> users = getAllUsers();
        status.setStatus("Extracting all ACLs", log);
        List<AclInfo> acls = getAllAcls();
        UpdateUtils.exportSecurityData(settings.getBaseDir(), users, acls, groups);
        status.setStatus("Security settings successfully exported", log);
    }

    private List<AclInfo> getAllAcls() {
        String aclsPath = getAclsJcrPath() + "/";

        // Access JCR directly without using OCM
        List<AclInfo> result = new ArrayList<AclInfo>();
        try {
            Node aclsNode = (Node) jcr.getSession().getItem(aclsPath);
            NodeIterator allAcls = aclsNode.getNodes();
            while (allAcls.hasNext()) {
                Node node = allAcls.nextNode();
                Property className = node.getProperty(OCM_CLASSNAME);
                if ("org.artifactory.security.Acl".equals(className.getString())) {
                    String jcrName = node.getProperty("jcrName").getString();
                    String repoKey = node.getProperty("repoKey").getString();
                    String includes = node.getProperty("includesPattern").getString();
                    String excludes = node.getProperty("excludesPattern").getString();
                    PermissionTargetInfo permissionTarget =
                            new PermissionTargetInfo(Text.unescapeIllegalJcrChars(jcrName), repoKey,
                                    includes, excludes);
                    AclInfo acl = new AclInfo(permissionTarget);
                    acl.setUpdatedBy(node.getProperty("updatedBy").getString());
                    result.add(acl);
                    NodeIterator children = node.getNodes();
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        className = child.getProperty(OCM_CLASSNAME);
                        if ("org.artifactory.security.Ace"
                                .equals(className.getString())) {
                            AceInfo ace = new AceInfo(
                                    child.getProperty("principal").getString(),
                                    child.getProperty("group").getBoolean(),
                                    Integer.parseInt(child.getProperty("mask").getString()));
                            UpdateUtils.updateAceMask(ace);
                            acl.getAces().add(ace);
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot extract ACLs from DB", e);
        }
        return result;
    }

    private List<UserInfo> getAllUsers() {
        String usersPath = getUsersJcrPath() + "/";

        // Access JCR directly without using OCM
        List<UserInfo> result = new ArrayList<UserInfo>();
        try {
            Node usersNode = (Node) jcr.getSession().getItem(usersPath);
            NodeIterator allUsers = usersNode.getNodes();
            while (allUsers.hasNext()) {
                Node node = allUsers.nextNode();
                Property className = node.getProperty(OCM_CLASSNAME);
                if ("org.artifactory.security.User".equals(className.getString())) {
                    String email = "";
                    if (node.hasProperty("email")) {
                        email = node.getProperty("email").getString();
                    }
                    UserInfo user = new UserInfo(
                            node.getProperty("username").getString(),
                            node.getProperty("password").getString(),
                            email,
                            node.getProperty("admin").getBoolean(),
                            node.getProperty("enabled").getBoolean(),
                            node.getProperty("updatableProfile").getBoolean(),
                            node.getProperty("accountNonExpired").getBoolean(),
                            node.getProperty("credentialsNonExpired").getBoolean(),
                            node.getProperty("accountNonLocked").getBoolean()
                    );
                    result.add(user);
                    if (node.hasProperty("groups")) {
                        Value[] groups = node.getProperty("groups").getValues();
                        for (Value group : groups) {
                            user.addGroup(group.getString());
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot extract Users from DB", e);
        }
        return result;
    }

    private List<GroupInfo> getAllGroups() {
        String groupsPath = getGroupsJcrPath() + "/";

        // Access JCR directly without using OCM
        List<GroupInfo> result = new ArrayList<GroupInfo>();
        try {
            Node groupsNode = (Node) jcr.getSession().getItem(groupsPath);
            NodeIterator allGroups = groupsNode.getNodes();
            while (allGroups.hasNext()) {
                Node node = allGroups.nextNode();
                Property className = node.getProperty(OCM_CLASSNAME);
                if ("org.artifactory.security.Group".equals(className.getString())) {
                    String description = "";
                    if (node.hasProperty("description")) {
                        description = node.getProperty("description").getString();
                    }
                    final GroupInfo groupInfo = new GroupInfo(
                            node.getProperty("groupName").getString(),
                            description
                    );
                    result.add(groupInfo);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot extract Groups from DB", e);
        }
        return result;
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
    }
}