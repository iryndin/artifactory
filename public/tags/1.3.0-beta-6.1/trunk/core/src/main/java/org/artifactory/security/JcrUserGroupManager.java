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
package org.artifactory.security;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Repository("userDetailsService")
public class JcrUserGroupManager implements UserGroupManager {

    private static final String USERS_KEY = "users";
    private static final String GROUPS_KEY = "groups";

    @Autowired
    private JcrService jcr;

    public static String getUsersJcrPath() {
        return JcrPath.get().getOcmClassJcrPath(USERS_KEY);
    }

    public static String getGroupsJcrPath() {
        return JcrPath.get().getOcmClassJcrPath(GROUPS_KEY);
    }

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(UserGroupManager.class);
    }

    public void init() {
        //Create the storage folders if they do not already exist
        Node confNode = jcr.getOrCreateUnstructuredNode(JcrPath.get().getOcmJcrRootPath());
        jcr.getOrCreateUnstructuredNode(confNode, USERS_KEY);
        jcr.getOrCreateUnstructuredNode(confNode, GROUPS_KEY);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Any relation to LDAP should be done here
    }

    public void destroy() {
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class};
    }

    @SuppressWarnings({"unchecked"})
    public Collection<User> getAllUsers(boolean includeAdmins) {
        ObjectContentManager ocm = getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(User.class);
        filter.setScope(getUsersJcrPath() + "/");
        Query query = queryManager.createQuery(filter);
        Collection<User> users = ocm.getObjects(query);
        return users;
    }

    public SimpleUser loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        ObjectContentManager ocm = getOcm();
        User user = (User) ocm.getObject(new User(username).getJcrPath());
        if (user != null) {
            return new SimpleUser(user.getInfo());
        } else {
            throw new UsernameNotFoundException("No such user: " + username + ".");
        }
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
        ObjectContentManager ocm = getOcm();
        Group group = (Group) ocm.getObject(new Group(groupName).getJcrPath());
        return group;
    }

    public boolean groupExists(String groupName) {
        return findGroup(groupName) != null;
    }

    public void updateGroup(Group group) {
        ObjectContentManager ocm = getOcm();
        ocm.update(group);
    }

    public boolean createGroup(Group group) {
        if (findGroup(group.getGroupName()) != null) {
            //Return false if the group already exists
            return false;
        } else {
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
