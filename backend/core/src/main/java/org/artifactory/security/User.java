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

package org.artifactory.security;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.ocm.OcmStorable;
import org.artifactory.security.jcr.JcrUserGroupManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Ocm storable user that doesn't extend any ss classes
 */
@Node(extend = OcmStorable.class)
public class User implements OcmStorable {

    private final MutableUserInfo info;

    /**
     * This dummy field is required to work around <a href="https://issues.apache.org/jira/browse/JCR-1928">ocm 1.5
     * bug</a> when using the annotation on the getters
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Collection(elementClassName = String.class, collectionConverter = MultiValueCollectionConverterImpl.class)
    private Set<String> groups;

    public User() {
        info = InfoFactoryHolder.get().createUser();
    }

    public User(String username) {
        info = InfoFactoryHolder.get().createUser(username);
    }

    public User(UserInfo user) {
        info = InfoFactoryHolder.get().copyUser(user);
    }

    public UserInfo getInfo() {
        return info;
    }

    @Override
    public String getJcrPath() {
        return JcrUserGroupManager.getUsersJcrPath() + "/" + getUsername();
    }

    @Override
    public void setJcrPath(String path) {
        //noop
    }

    @Field
    public String getUsername() {
        return info.getUsername();
    }

    public void setUsername(String username) {
        info.setUsername(username);
    }

    @Field
    public String getPassword() {
        return info.getPassword();
    }

    public void setPassword(String password) {
        info.setPassword(password);
    }

    @Field
    public String getEmail() {
        return info.getEmail();
    }

    public void setEmail(String email) {
        info.setEmail(email);
    }

    @Field
    public String getRealm() {
        return info.getRealm();
    }

    public void setRealm(String realm) {
        info.setRealm(realm);
    }

    @Field
    public String getPrivateKey() {
        return info.getPrivateKey();
    }

    public void setPrivateKey(String privateKey) {
        info.setPrivateKey(privateKey);
    }

    @Field
    public String getPublicKey() {
        return info.getPublicKey();
    }

    public void setPublicKey(String publicKey) {
        info.setPublicKey(publicKey);
    }

    @Field
    public String getGenPasswordKey() {
        return info.getGenPasswordKey();
    }

    public void setGenPasswordKey(String genPasswordKey) {
        info.setGenPasswordKey(genPasswordKey);
    }

    @Field
    public boolean isAdmin() {
        return info.isAdmin();
    }

    public void setAdmin(boolean admin) {
        info.setAdmin(admin);
    }

    @Field
    public boolean isEnabled() {
        return info.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        info.setEnabled(enabled);
    }

    @Field
    public boolean isUpdatableProfile() {
        return info.isUpdatableProfile();
    }

    public void setUpdatableProfile(boolean updatableProfile) {
        info.setUpdatableProfile(updatableProfile);
    }

    @Field
    public boolean isAccountNonExpired() {
        return info.isAccountNonExpired();
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        info.setAccountNonExpired(accountNonExpired);
    }

    @Field
    public boolean isAccountNonLocked() {
        return info.isAccountNonLocked();
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        info.setAccountNonLocked(accountNonLocked);
    }

    public boolean isTransientUser() {
        return info.isTransientUser();
    }

    public void setTransientUser(boolean transientUser) {
        info.setTransientUser(transientUser);
    }

    @Field
    public boolean isCredentialsNonExpired() {
        return info.isCredentialsNonExpired();
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        info.setCredentialsNonExpired(credentialsNonExpired);
    }

    //@Collection(elementClassName = String.class, collectionConverter = MultiValueCollectionConverterImpl.class)

    public Set<String> getGroups() {
        //Disregard transient realm information - we persist only the group names
        Set<String> groupNames = new HashSet<String>();
        for (UserGroupInfo groupInfo : info.getGroups()) {
            groupNames.add(groupInfo.getGroupName());
        }
        return groupNames;
    }

    public void setGroups(Set<String> groups) {
        info.setInternalGroups(groups);
    }

    @Field
    public long getLastLoginTimeMillis() {
        return info.getLastLoginTimeMillis();
    }

    public void setLastLoginTimeMillis(long lastLoginTimeMillis) {
        info.setLastLoginTimeMillis(lastLoginTimeMillis);
    }

    @Field
    public String getLastLoginClientIp() {
        return info.getLastLoginClientIp();
    }

    public void setLastLoginClientIp(String lastLoginClientIp) {
        info.setLastLoginClientIp(lastLoginClientIp);
    }

    @Field
    public long getLastAccessTimeMillis() {
        return info.getLastAccessTimeMillis();
    }

    public void setLastAccessTimeMillis(long lastAccessTimeMillis) {
        info.setLastAccessTimeMillis(lastAccessTimeMillis);
    }

    @Field
    public String getLastAccessClientIp() {
        return info.getLastAccessClientIp();
    }

    public void setLastAccessClientIp(String lastAccessClientIp) {
        info.setLastAccessClientIp(lastAccessClientIp);
    }
}