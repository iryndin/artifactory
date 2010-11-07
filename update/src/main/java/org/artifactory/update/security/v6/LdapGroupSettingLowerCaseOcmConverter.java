/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.update.security.v6;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AclManager;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Map;
import java.util.Set;

/**
 * This converter does actually 2 conversions, one is to convert all property with the name "string" which used to
 * denote group names into "userGroup". The other is to make all users lowercase and merge their user groups.
 *
 * @author Tomer Cohen
 */
public class LdapGroupSettingLowerCaseOcmConverter implements ConfigurationConverter<Session> {

    private static final Logger log = LoggerFactory.getLogger(LdapGroupSettingLowerCaseOcmConverter.class);

    public void convert(Session session) {
        try {
            log.info("Stating LdapGroupSettingLowerCaseOcmConverter");
            //First convert all group names to lowercase
            Node groupsNode = getJcrGroupsPath(session);
            NodeIterator groupNodeIterator = groupsNode.getNodes();
            Map<String, Node> foundGroups = Maps.newHashMap();
            while (groupNodeIterator.hasNext()) {
                Node groupNode = groupNodeIterator.nextNode();
                Property groupNameProperty = groupNode.getProperty("groupName");
                String desc = null;
                if (groupNode.hasProperty("description")) {
                    desc = groupNode.getProperty("description").getString();
                }
                String groupName = groupNameProperty.getString();
                String lowercaseGroupName = groupName.toLowerCase();
                if (groupName.equals(lowercaseGroupName)) {
                    // Group name already lowercase,
                    // no OCM modification to do, just check previous data node if needed
                    if (foundGroups.containsKey(lowercaseGroupName)) {
                        groupNodeAlreadyFound(foundGroups, groupNode, desc, lowercaseGroupName);
                    } else {
                        foundGroups.put(lowercaseGroupName, groupNode);
                    }
                } else {
                    if (foundGroups.containsKey(lowercaseGroupName)) {
                        groupNodeAlreadyFound(foundGroups, groupNode, desc, lowercaseGroupName);
                    } else {
                        // Modify OCM properties and path to match lowercase version
                        groupNameProperty.setValue(lowercaseGroupName);
                        String originalPath = groupNode.getPath();
                        moveJcrNodes(session, originalPath);
                        foundGroups.put(lowercaseGroupName, groupNode);
                    }
                }
            }

            //Third convert all users to lowercase (and filtering all those groups that don't exist in the system anymore)
            Node usersNode = getJcrUserPath(session);
            NodeIterator usersNodesIt = usersNode.getNodes();
            Map<String, Node> userNodes = Maps.newHashMap();
            while (usersNodesIt.hasNext()) {
                Node userNode = usersNodesIt.nextNode();
                Property userNameProperty = userNode.getProperty("username");
                boolean admin = userNode.getProperty("admin").getBoolean();
                String email = null;
                if (userNode.hasProperty("email")) {
                    email = userNode.getProperty("email").getString();
                }
                String username = userNameProperty.getString();
                String lowerCaseUserName = username.toLowerCase();
                if (lowerCaseUserName.equals(username)) {
                    // User name already lowercase,
                    // no OCM modification to do, just check previous data node if needed
                    if (userNodes.containsKey(lowerCaseUserName)) {
                        userNodeAlreadyFound(userNodes, userNode, email, lowerCaseUserName, admin);
                    } else {
                        userNodes.put(lowerCaseUserName, userNode);
                        addGroupsToUser(userNode, userNode);
                    }
                } else {
                    if (userNodes.containsKey(lowerCaseUserName)) {
                        userNodeAlreadyFound(userNodes, userNode, email, lowerCaseUserName, admin);
                    } else {
                        // User name changed need to do all the OCM modifications (property and path).
                        userNameProperty.setValue(lowerCaseUserName);
                        if (userNode.hasProperty("groups")) {
                            changeUserGroupNames(userNode);
                        }
                        String originalPath = userNode.getPath();
                        moveJcrNodes(session, originalPath);
                        userNodes.put(lowerCaseUserName, userNode);
                    }
                }
            }
            //Remove the group names that no-longer exist from the ACLs
            mergeAcls(session);
            session.save();

            AclManager aclManager = ContextHelper.get().beanForType(AclManager.class);
            /**
             * Reload ACLs since this code is executed in the security service, which is after the ACL cache has already
             * been initialized
             */
            aclManager.reloadAcls();

            log.info("Finished LdapGroupSettingLowerCaseOcmConverter");
        }
        catch (RepositoryException e) {
            log.error("Error while converting OCM Group settings", e);
        }
    }

    private void changeUserGroupNames(Node userNode) throws RepositoryException {
        Property userGroupProperty = userNode.getProperty("groups");
        Value[] userGroupNames = userGroupProperty.getValues();
        Set<String> otherGroups = Sets.newHashSet();
        for (Value groupName : userGroupNames) {
            otherGroups.add(groupName.getString().toLowerCase());
        }
        String[] lowerCaseGroupNames = new String[otherGroups.size()];
        otherGroups.toArray(lowerCaseGroupNames);
        userNode.setProperty("groups", lowerCaseGroupNames);
    }

    private void moveJcrNodes(Session session, String originalPath) throws RepositoryException {
        String newPath;
        if (originalPath.endsWith("/")) {
            originalPath = originalPath.substring(0, originalPath.length() - 1);
            newPath = createNewPath(originalPath);
        } else {
            originalPath = originalPath.substring(0, originalPath.length());
            newPath = createNewPath(originalPath);
        }
        session.move(originalPath, newPath);
    }

    private String createNewPath(String originalPath) {
        String newPath;
        int indexOfName = originalPath.lastIndexOf('/');
        String name = originalPath.substring(indexOfName, originalPath.length());
        String originalPathWithoutEnd = StringUtils.removeEnd(originalPath, name);
        newPath = originalPathWithoutEnd + name.toLowerCase();
        return newPath;
    }

    private void userNodeAlreadyFound(Map<String, Node> foundUsers, Node userNode, String email,
                                      String lowerCaseUserName, boolean admin)
            throws RepositoryException {
        Node otherUserNode = foundUsers.get(lowerCaseUserName);
        if (otherUserNode == null) {
            throw new IllegalStateException("Cannot apply merge user on " + userNode + " if not already found!");
        }
        if (StringUtils.isNotBlank(email)) {
            if (otherUserNode.hasProperty("email")) {
                Property otherEmailProperty = otherUserNode.getProperty("email");
                String otherEmail = otherEmailProperty.getString();
                if (StringUtils.isBlank(otherEmail)) {
                    otherEmailProperty.setValue(email);
                }
            }
        }
        if (admin) {
            Property otherAdminProperty = otherUserNode.getProperty("admin");
            otherAdminProperty.setValue(true);
        }
        addGroupsToUser(userNode, otherUserNode);
        userNode.remove();
    }

    private void groupNodeAlreadyFound(Map<String, Node> foundGroups, Node groupNode, String desc,
                                       String lowercaseGroupName) throws RepositoryException {
        if (StringUtils.isNotBlank(desc)) {
            Node otherGroupNode = foundGroups.get(lowercaseGroupName);
            if (otherGroupNode.hasProperty("description")) {
                Property otherDescProp = otherGroupNode.getProperty("description");
                String otherDesc = otherDescProp.getString();
                if (StringUtils.isBlank(otherDesc)) {
                    otherDescProp.setValue(desc);
                }
            }
        }
        groupNode.remove();
    }

    /**
     * Remove redundant groups from ACLs which may have different case variations in their group name
     */
    private void mergeAcls(Session session) throws RepositoryException {
        Node aclNodes = getJcrAclPath(session);
        NodeIterator aclNodeIterator = aclNodes.getNodes();
        while (aclNodeIterator.hasNext()) {
            Map<String, Node> aces = Maps.newHashMap();
            Node aclNode = aclNodeIterator.nextNode();
            Node acesNode = aclNode.getNode("aces");
            NodeIterator nodeIterator = acesNode.getNodes();
            while (nodeIterator.hasNext()) {
                Node aceNode = nodeIterator.nextNode();
                Property principalProperty = aceNode.getProperty("principal");
                String principal = principalProperty.getString();
                boolean isGroup = aceNode.getProperty("group").getBoolean();
                String lowerCasePrincipal = principal.toLowerCase();
                String aceKey = lowerCasePrincipal + ":" + isGroup;
                if (principal.equals(lowerCasePrincipal)) {
                    // Principal name already lowercase,
                    // no OCM modification to do, just check previous data node if needed
                    if (aces.containsKey(aceKey)) {
                        aceNodeAlreadyFound(aces, aceNode, aceKey);
                    } else {
                        aces.put(aceKey, aceNode);
                    }
                } else {
                    if (aces.containsKey(aceKey)) {
                        aceNodeAlreadyFound(aces, aceNode, aceKey);
                    } else {
                        // Convert principal to lowercase
                        principalProperty.setValue(lowerCasePrincipal);
                        String originalPath = aceNode.getPath();
                        moveJcrNodes(session, originalPath);
                        aces.put(aceKey, aceNode);
                    }
                }
            }
        }
    }

    private void aceNodeAlreadyFound(Map<String, Node> aces, Node aceNode, String aceKey) throws RepositoryException {
        Node otherAceNode = aces.get(aceKey);
        Property otherMaskProperty = otherAceNode.getProperty("mask");
        int originalMask = (int) otherMaskProperty.getLong();
        int currentMask = (int) aceNode.getProperty("mask").getLong();
        otherMaskProperty.setValue(currentMask | originalMask);
        aceNode.remove();
    }

    /**
     * Add a set of merged groups to a user
     */
    private void addGroupsToUser(Node toBeDeletedUserNode, Node toBeKeptUserNode)
            throws RepositoryException {
        Set<String> newGroupNames = getUserGroups(toBeDeletedUserNode);
        Set<String> existingGroupNames = getUserGroups(toBeKeptUserNode);
        existingGroupNames.addAll(newGroupNames);
        String[] groupNames = new String[existingGroupNames.size()];
        existingGroupNames.toArray(groupNames);
        toBeKeptUserNode.setProperty("groups", groupNames);
    }

    /**
     * Get the user's defined groups.
     */
    private Set<String> getUserGroups(Node userNode) throws RepositoryException {
        Set<String> groups = Sets.newHashSet();
        Property userGroups = userNode.getProperty("groups");
        Value[] userGroupNames = userGroups.getValues();
        for (Value group : userGroupNames) {
            groups.add(group.getString().toLowerCase());
        }
        return groups;
    }

    private static Node getJcrGroupsPath(Session session) throws RepositoryException {
        return (Node) session.getItem("/configuration/groups/");
    }

    private static Node getJcrUserPath(Session session) throws RepositoryException {
        return (Node) session.getItem("/configuration/users/");
    }

    private static Node getJcrAclPath(Session session) throws RepositoryException {
        return (Node) session.getItem("/configuration/acls/");
    }
}
