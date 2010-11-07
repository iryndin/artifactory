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

package org.artifactory.security.interceptor;

import org.artifactory.api.security.SecurityInfo;
import org.artifactory.interceptor.Interceptors;
import org.artifactory.jcr.JcrService;
import org.artifactory.spring.Reloadable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Yossi Shaul
 */
@Service
@Reloadable(beanClass = SecurityConfigurationChangesInterceptors.class, initAfter = JcrService.class)
public class SecurityConfigurationChangesInterceptorsImpl extends Interceptors<SecurityConfigurationChangesInterceptor>
        implements
        SecurityConfigurationChangesInterceptors {

    public void onUserAdd(String user) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onUserAdd(user);
        }
    }

    public void onUserDelete(String user) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onUserDelete(user);
        }
    }

    public void onAddUsersToGroup(String groupName, List<String> usernames) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onAddUsersToGroup(groupName, usernames);
        }
    }

    public void onRemoveUsersFromGroup(String groupName, List<String> usernames) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onRemoveUsersFromGroup(groupName, usernames);
        }
    }

    public void onGroupAdd(String group) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onGroupAdd(group);
        }
    }

    public void onGroupDelete(String group) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onGroupDelete(group);
        }
    }

    public void onPermissionsAdd() {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onPermissionsAdd();
        }
    }

    public void onPermissionsUpdate() {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onPermissionsUpdate();
        }
    }

    public void onPermissionsDelete() {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onPermissionsDelete();
        }
    }

    public void onBeforeSecurityImport(SecurityInfo securityInfo) {
        for (SecurityConfigurationChangesInterceptor interceptor : this) {
            interceptor.onBeforeSecurityImport(securityInfo);
        }
    }
}