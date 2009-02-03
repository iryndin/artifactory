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
import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.security.config.User;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrUserDetailsService implements UserDetailsService, InitializingBean,
        ApplicationContextAware {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrUserDetailsService.class);

    private static final String USERS_KEY = "users";

    private ArtifactoryApplicationContext applicationContext;

    private ExtendedAclService aclService;

    public static String getUsersJcrPath() {
        return JcrPath.get().getOcmClassJcrPath(USERS_KEY);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ArtifactoryApplicationContext) applicationContext;
    }

    public void setAclService(JcrAclService aclService) {
        this.aclService = aclService;
    }

    public void afterPropertiesSet() throws Exception {
        //Create the storage folder if it is not there
        JcrWrapper jcr = applicationContext.getJcr();
        jcr.getOrCreateUnstructuredNode(getUsersJcrPath());
    }

    public List<SimpleUser> getAllUsers() {
        return getAllUsers(true);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
    public List<SimpleUser> getAllUsers(boolean includeAdmins) {
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(User.class);
        filter.setScope(getUsersJcrPath() + "/");
        Query query = queryManager.createQuery(filter);
        Collection results = ocm.getObjects(query);
        ArrayList<SimpleUser> users = new ArrayList<SimpleUser>(results.size());
        for (Object result : results) {
            User user = (User) result;
            SimpleUser simpleUser = user.toSimpleUser();
            if (!includeAdmins) {
                if (!user.isAdmin()) {
                    //Only include non admin users
                    users.add(simpleUser);
                }
            } else {
                users.add(simpleUser);
            }
        }
        return users;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public SimpleUser loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        User user = (User) ocm.getObject(new User(username).getJcrPath());
        SimpleUser simpleUser = user != null ? user.toSimpleUser() : null;
        return simpleUser;
    }

    public boolean createUser(SimpleUser user) {
        String username = user.getUsername();
        if (loadUserByUsername(username) != null) {
            //Return false if the user already exists
            return false;
        } else {
            JcrWrapper jcr = applicationContext.getJcr();
            ObjectContentManager ocm = jcr.getOcm();
            ocm.insert(new User(user));
            ocm.save();
            return true;
        }
    }

    public void updateUser(SimpleUser user) {
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        ocm.update(new User(user));
        ocm.save();
    }

    public void removeUser(SimpleUser user) {
        removeUser(user.getUsername());
    }

    public void removeUser(String username) {
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        User user = new User(username);
        //Remove all the user acls
        List<RepoPathAcl> acls = aclService.getAllAcls();
        PrincipalSid sid = new PrincipalSid(username);
        for (RepoPathAcl acl : acls) {
            acl.removePrincipalAces(sid);
            aclService.updateAcl(acl);
        }
        //Remove the user
        ocm.remove(user);
        ocm.save();
    }
}
