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

package org.artifactory.update.security.v3;

import org.apache.jackrabbit.util.Text;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.factory.InfoFactory;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.security.*;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.artifactory.security.PermissionTargetInfo.ANY_REMOTE_PERMISSION_TARGET_NAME;
import static org.artifactory.security.PermissionTargetInfo.ANY_REMOTE_REPO;

/**
 * Converts the security ocm data from security version 3 to version 4. This converter performs these actions: <ol> <li>
 * Converts the acls from one repo key to multiple repo keys per target info. Adds the new any remote default permission
 * <li> Reads all data, removes it and saves it using ocm (needed due to a change in the ocm version) </ol>
 *
 * @author Noam Tenne
 */
public class OcmStorageConverter implements ConfigurationConverter<Session> {

    private final static Logger log = LoggerFactory.getLogger(OcmStorageConverter.class);

    protected static final String NODE_CONFIGURATION = "configuration";

    private static final String ACLS_KEY = "acls";
    private static final String USERS_KEY = "users";
    private static final String GROUPS_KEY = "groups";

    protected final InfoFactory factory = InfoFactoryHolder.get();

    @Override
    public void convert(Session session) {
        log.info("Starting OcmStorageConverter");
        List<UserInfo> users = getUsers(session);
        List<GroupInfo> groups = getGroups(session);
        List<AclInfo> acls = getAcls(session);
        SecurityInfo descriptor = factory.createSecurityInfo(users, groups, acls);
        try {
            removeChildren(getAclsNode(session));
            removeChildren(getGroupsNode(session));
            removeChildren(getUsersNode(session));
            session.save();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot remove old security data.", e);
        }
        //Save the new security data
        SecurityService securityService = ContextHelper.get().beanForType(SecurityService.class);
        securityService.importSecurityData(descriptor);
        log.info("Finished OcmStorageConverter");
    }

    private List<AclInfo> getAcls(Session session) {
        // Access JCR directly without using OCM
        List<AclInfo> result = new ArrayList<AclInfo>();
        try {
            Node aclsNode = getAclsNode(session);
            NodeIterator allAcls = aclsNode.getNodes();
            while (allAcls.hasNext()) {
                Node node = allAcls.nextNode();
                String jcrName = node.getProperty("jcrName").getString();
                // up to version 3 acl contained one repo key
                String repoKey = node.getProperty("repoKey").getString();
                String includes = node.getProperty("includesPattern").getString();
                String excludes = node.getProperty("excludesPattern").getString();
                MutablePermissionTargetInfo permissionTarget = factory.createPermissionTarget(
                        Text.unescapeIllegalJcrChars(jcrName), Arrays.asList(repoKey));
                permissionTarget.setIncludesPattern(includes);
                permissionTarget.setExcludesPattern(excludes);
                MutableAclInfo acl = factory.createAcl(permissionTarget);
                if (node.hasProperty("updatedBy")) {
                    acl.setUpdatedBy(node.getProperty("updatedBy").getString());
                } else {
                    acl.setUpdatedBy(SecurityService.USER_SYSTEM);
                }
                result.add(acl);
                NodeIterator children = node.getNodes();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    MutableAceInfo ace = factory.createAce(child.getProperty("principal").getString(),
                            child.getProperty("group").getBoolean(),
                            Integer.parseInt(child.getProperty("mask").getString()));
                    acl.getMutableAces().add(ace);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot convert stored acls", e);
        }

        addDefaultAnyRemoteAcl(result);

        return result;
    }

    private void addDefaultAnyRemoteAcl(List<AclInfo> acls) {
        // if the default Anything permission target exists and the anonymous user read permission is set, add
        // the new default "any remote" premission and grant read+deploy permissions on it to anonymous
        boolean shouldAddDefaultAnyRemote = false;
        for (AclInfo acl : acls) {
            if (acl.getPermissionTarget().getName().equals(PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME)) {
                for (AceInfo ace : acl.getAces()) {
                    if (ace.getPrincipal().equals(UserInfo.ANONYMOUS) && ace.canRead()) {
                        shouldAddDefaultAnyRemote = true;
                    }
                }
                break;
            }
        }

        if (shouldAddDefaultAnyRemote) {
            log.debug("Adding default any remote permissions");
            // create or update read and deploy permissions on all remote repos
            MutableAceInfo anonAnyRemoteAce = factory.createAce(UserInfo.ANONYMOUS, false, 0);
            anonAnyRemoteAce.setRead(true);
            anonAnyRemoteAce.setDeploy(true);
            PermissionTargetInfo anyRemoteTarget = factory.createPermissionTarget(ANY_REMOTE_PERMISSION_TARGET_NAME,
                    Arrays.asList(ANY_REMOTE_REPO));

            Set<AceInfo> aces = new HashSet<AceInfo>();
            aces.add(anonAnyRemoteAce);
            MutableAclInfo anyRemoteAcl = factory.createAcl(anyRemoteTarget, aces, SecurityService.USER_SYSTEM);
            acls.add(anyRemoteAcl);
        }
    }

    private List<UserInfo> getUsers(Session session) {
        // Access JCR directly without using OCM
        List<UserInfo> result = new ArrayList<UserInfo>();
        try {
            Node usersNode = getUsersNode(session);
            NodeIterator allUsers = usersNode.getNodes();
            while (allUsers.hasNext()) {
                Node node = allUsers.nextNode();
                UserInfoBuilder builder = new UserInfoBuilder(node.getProperty("username").getString());
                builder.password(node.getProperty("password").getString())
                        .email(node.hasProperty("email") ? node.getProperty("email").getString() : "")
                        .admin(node.getProperty("admin").getBoolean())
                        .enabled(node.getProperty("enabled").getBoolean())
                        .updatableProfile(node.getProperty("updatableProfile").getBoolean());
                MutableUserInfo user = builder.build();
                result.add(user);
                if (node.hasProperty("groups")) {
                    Value[] groups = node.getProperty("groups").getValues();
                    for (Value group : groups) {
                        user.addGroup(group.getString());
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot convert stored users", e);
        }
        return result;
    }

    private List<GroupInfo> getGroups(Session session) {
        // Access JCR directly without using OCM
        List<GroupInfo> result = new ArrayList<GroupInfo>();
        try {
            Node groupsNode = getGroupsNode(session);
            NodeIterator allGroups = groupsNode.getNodes();
            while (allGroups.hasNext()) {
                Node node = allGroups.nextNode();
                String description = "";
                Property descriptionProperty;
                try {
                    descriptionProperty = node.getProperty("description");
                } catch (PathNotFoundException e) {
                    descriptionProperty = null;
                }
                if ((descriptionProperty != null) && (descriptionProperty.getString() != null)) {
                    description = descriptionProperty.getString();
                }

                final MutableGroupInfo groupInfo = factory.createGroup(node.getProperty("groupName").getString());
                groupInfo.setDescription(description);
                groupInfo.setNewUserDefault(node.getProperty("newUserDefault").getBoolean());
                result.add(groupInfo);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot convert stored groups", e);
        }
        return result;
    }

    private void removeChildren(Node node) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node chNode = (Node) nodes.next();
            chNode.remove();
        }
    }

    private Node getUsersNode(Session session) throws RepositoryException {
        String path = getOcmClassJcrPath(USERS_KEY) + "/";
        return (Node) session.getItem(path);
    }

    private Node getGroupsNode(Session session) throws RepositoryException {
        String path = getOcmClassJcrPath(GROUPS_KEY) + "/";
        return (Node) session.getItem(path);
    }

    private Node getAclsNode(Session session) throws RepositoryException {
        String path = getOcmClassJcrPath(ACLS_KEY) + "/";
        return (Node) session.getItem(path);
    }

    private static String getOcmClassJcrPath(String classKey) {
        return "/" + NODE_CONFIGURATION + "/" + classKey;
    }
}
