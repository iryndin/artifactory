/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.security.jcr;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.artifactory.api.security.GroupNotFoundException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.security.Group;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.User;
import org.artifactory.security.UserGroupManager;
import org.artifactory.security.UserInfo;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.jcr.Node;
import java.util.Collection;

/**
 * @author Yoav Landman
 */
@Repository("userDetailsService")
@Reloadable(beanClass = UserGroupManager.class, initAfter = JcrService.class)
public class JcrUserGroupManager implements UserGroupManager {

    private static final Logger log = LoggerFactory.getLogger(JcrUserGroupManager.class);

    private static final String USERS_KEY = "users";
    private static final String GROUPS_KEY = "groups";

    @Autowired
    private JcrService jcr;

    // cache the anonymous user which is accessed frequently
    private SimpleUser anonymousUser;

    public static String getUsersJcrPath() {
        return PathFactoryHolder.get().getConfigPath(USERS_KEY);
    }

    public static String getGroupsJcrPath() {
        return PathFactoryHolder.get().getConfigPath(GROUPS_KEY);
    }

    public void init() {
        //Create the storage folders if they do not already exist
        Node confNode = jcr.getOrCreateUnstructuredNode(PathFactoryHolder.get().getConfigurationRootPath());
        jcr.getOrCreateUnstructuredNode(confNode, USERS_KEY);
        jcr.getOrCreateUnstructuredNode(confNode, GROUPS_KEY);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Any relation to LDAP should be done here
        anonymousUser = null;
    }

    public void destroy() {
        anonymousUser = null;
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @SuppressWarnings({"unchecked"})
    public Collection<User> getAllUsers() {
        ObjectContentManager ocm = getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(User.class);
        filter.setScope(getUsersJcrPath() + "/");
        Query query = queryManager.createQuery(filter);
        Collection<User> users = ocm.getObjects(query);
        return users;
    }

    public boolean userExists(String username) {
        if (UserInfo.ANONYMOUS.equals(username)) {
            return true;
        }

        if (StringUtils.hasLength(username)) {
            return getOcm().objectExists(new User(username).getJcrPath());
        }
        return false;
    }

    public SimpleUser loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        if (UserInfo.ANONYMOUS.equals(username) && anonymousUser != null) {
            return anonymousUser;
        }

        log.debug("Loading user {} from storage.", username);

        if (StringUtils.hasLength(username)) {
            ObjectContentManager ocm = getOcm();
            User user = (User) ocm.getObject(new User(username).getJcrPath());
            if (user != null) {
                SimpleUser simpleUser = new SimpleUser(user.getInfo());
                if (UserInfo.ANONYMOUS.equals(username)) {
                    anonymousUser = simpleUser;
                }
                return simpleUser;
            }
        }
        throw new UsernameNotFoundException("No such user: " + username + ".");
    }

    @SuppressWarnings({"unchecked"})
    public Collection<Group> getAllGroups() {
        ObjectContentManager ocm = getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(Group.class);
        filter.setScope(getGroupsJcrPath() + "/");
        Query query = queryManager.createQuery(filter);
        Collection<Group> groups = ocm.getObjects(query);
        return groups;
    }

    public Group findGroup(String groupName) {
        log.debug("Loading group {} from storage.", groupName);

        if (StringUtils.hasLength(groupName)) {
            ObjectContentManager ocm = getOcm();
            Group group = (Group) ocm.getObject(new Group(groupName).getJcrPath());
            if (group != null) {
                return group;
            }
        }

        throw new GroupNotFoundException("No such group: " + groupName + ".");
    }

    public void updateGroup(Group group) {
        ObjectContentManager ocm = getOcm();
        ocm.update(group);
    }

    public void deleteAllGroupsAndUsers() {
        jcr.delete(getGroupsJcrPath());
        jcr.delete(getUsersJcrPath());
        jcr.getOrCreateUnstructuredNode(getGroupsJcrPath());
        jcr.getOrCreateUnstructuredNode(getUsersJcrPath());
        jcr.getManagedSession().save();
        anonymousUser = null;
    }

    public boolean createGroup(Group group) {
        try {
            findGroup(group.getGroupName());
            //Return false if the group already exists
            return false;
        } catch (Exception e) {
            ObjectContentManager ocm = getOcm();
            ocm.insert(group);
            ocm.save();
            return true;
        }
    }

    public void removeGroup(String groupName) {
        ObjectContentManager ocm = getOcm();
        Group group = new Group(groupName);
        ocm.remove(group);
        ocm.save();
    }

    public boolean createUser(SimpleUser user) {
        String username = user.getUsername();
        try {
            loadUserByUsername(username);
            //Return false if the user already exists
            return false;
        } catch (UsernameNotFoundException e) {
            ObjectContentManager ocm = getOcm();
            ocm.insert(new User(user.getDescriptor()));
            ocm.save();
            return true;
        }
    }

    public void updateUser(SimpleUser user) {
        // remove and then create see http://issues.jfrog.org/jira/browse/RTFACT-1740
        removeUser(user);
        createUser(user);
    }

    public void removeUser(SimpleUser user) {
        removeUser(user.getUsername());
    }

    public void removeUser(String username) {
        ObjectContentManager ocm = getOcm();
        User user = new User(username);
        ocm.remove(user);
        ocm.save();
    }

    private ObjectContentManager getOcm() {
        ObjectContentManager ocm = jcr.getOcm();
        return ocm;
    }
}
