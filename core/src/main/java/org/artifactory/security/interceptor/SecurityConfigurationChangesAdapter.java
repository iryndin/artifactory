/*
 * Copyright 2009 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.security.interceptor;

import org.artifactory.api.security.SecurityInfo;

import java.util.List;

/**
 * A default implementation adapter for the security configuration changes interceptors
 *
 * @author Noam Y. Tenne
 */
public abstract class SecurityConfigurationChangesAdapter implements SecurityConfigurationChangesInterceptor {

    public void onUserAdd(String user) {
    }

    public void onUserDelete(String user) {
    }

    public void onAddUsersToGroup(String groupName, List<String> usernames) {
    }

    public void onRemoveUsersFromGroup(String groupName, List<String> usernames) {
    }

    public void onGroupAdd(String group) {
    }

    public void onGroupDelete(String group) {
    }

    public void onPermissionsAdd() {
    }

    public void onPermissionsUpdate() {
    }

    public void onPermissionsDelete() {
    }

    public void onBeforeSecurityImport(SecurityInfo securityInfo) {
    }
}