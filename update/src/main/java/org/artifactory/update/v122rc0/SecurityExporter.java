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
package org.artifactory.update.v122rc0;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.update.VersionsHolder;
import org.artifactory.update.utils.UpdateUtils;
import org.springframework.jdbc.object.MappingSqlQuery;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Security Export based on SQL in the original AppDB. Works until version 125u1.
 *
 * @author freds
 * @date Aug 14, 2008
 */
public class SecurityExporter implements ImportableExportable {
    private static final Logger LOGGER =
            LogManager.getLogger(SecurityExporter.class);

    public static final String ALL_USERS_QUERY = "SELECT * FROM users";
    public static final String ALL_ADMIN_QUERY =
            "SELECT username FROM authorities where authority = 'ADMIN'";
    public static final String ALL_PERMISSION_TARGET_QUERY = "SELECT * FROM acl_object_identity";
    public static final String ALL_ACE_QUERY = "SELECT * FROM acl_permission";

    private DataSource dataSource;
    protected MappingSqlQuery allUsersMapping;
    protected MappingSqlQuery allAdmins;
    protected MappingSqlQuery allPermissionTargets;
    protected MappingSqlQuery allAces;
    private Set<String> admins;
    private Map<Long, AclInfo> allAcls;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        allUsersMapping = new AllUsersMapping();
        allAdmins = new AllAdmins();
        allPermissionTargets = new AllPermissionTargets();
        allAces = new AllAcls();
    }

    @SuppressWarnings({"unchecked"})
    public List<UserInfo> getAllUsers() {
        List<UserInfo> users = allUsersMapping.execute();
        return users;
    }

    private Set<String> getAdmins() {
        if (admins == null) {
            //noinspection unchecked
            admins = new HashSet<String>(allAdmins.execute());
        }
        return admins;
    }

    /**
     * Export the security data as xml using xstream
     *
     * @param status
     */
    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Extracting all users", LOGGER);
        List<UserInfo> users = getAllUsers();
        // Test if hash password conversion needed
        if (VersionsHolder.getOriginalVersion().getRevision() < 913) {
            LOGGER.info("User passwords need hash conversion.");
            for (UserInfo user : users) {
                String oldPassword = user.getPassword();
                String newPassword = DigestUtils.md5Hex(oldPassword);
                user.setPassword(newPassword);
                LOGGER.info(
                        "Password successfully updated for user '" + user.getUsername() + "'.");
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User passwords do not need hash conversion.");
            }
        }
        status.setStatus("Extracting all ACLs", LOGGER);
        allAcls = new HashMap<Long, AclInfo>();
        allPermissionTargets.execute();
        allAces.execute();
        ArrayList<AclInfo> acls = new ArrayList<AclInfo>(allAcls.values());
        status.setCallback(UpdateUtils.exportSecurityData(settings.getBaseDir(), users, acls));
        status.setStatus("Security settings successfully exported", LOGGER);
    }

    /**
     * Query object to look up all users.
     */
    protected class AllUsersMapping extends MappingSqlQuery {
        /*
	username VARCHAR(50) NOT NULL PRIMARY KEY,
	password VARCHAR(50) NOT NULL,
	enabled SMALLINT NOT NULL,
	updatable_profile SMALLINT NOT NULL
         */
        private ResultSetMetaData rsmd = null;
        private int enabledColumn = 0;
        private int updatedProfileColumn = 0;

        protected AllUsersMapping() {
            super(getDataSource(), ALL_USERS_QUERY);
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum)
                throws SQLException {
            if (rsmd == null) {
                rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    if ("enabled".equalsIgnoreCase(rsmd.getColumnName(i))) {
                        enabledColumn = i;
                    } else if ("updatable_profile".equalsIgnoreCase(rsmd.getColumnName(i))) {
                        updatedProfileColumn = i;
                    }
                }
            }
            String username = rs.getString("username");
            String password = rs.getString("password");
            boolean enabled = true;
            if (enabledColumn != 0) {
                enabled = rs.getShort(enabledColumn) != 0;
            }
            boolean updProfile = true;
            if (updatedProfileColumn != 0) {
                updProfile = rs.getShort(updatedProfileColumn) != 0;
            }
            UserInfo user = new UserInfo(username);
            user.setPassword(password);
            user.setEnabled(enabled);
            user.setUpdatableProfile(updProfile);
            user.setAdmin(getAdmins().contains(username));
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);

            return user;
        }
    }

    private class AllAdmins extends MappingSqlQuery {
        /*
	username VARCHAR(50) NOT NULL,
	authority VARCHAR(50) NOT NULL
         */
        protected AllAdmins() {
            super(getDataSource(), ALL_ADMIN_QUERY);
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
            return rs.getString(1);
        }
    }

    private class AllPermissionTargets extends MappingSqlQuery {
        /*
        CREATE TABLE acl_object_identity (
             id BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,
             object_identity VARCHAR(250) NOT NULL,
             parent_object BIGINT,
             acl_class VARCHAR(250) NOT NULL,
             CONSTRAINT unique_object_identity UNIQUE(object_identity),
             FOREIGN KEY (parent_object) REFERENCES acl_object_identity(id)
        );
        */

        private AllPermissionTargets() {
            super(getDataSource(), ALL_PERMISSION_TARGET_QUERY);
            compile();
        }

        protected Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            String objectIdentity = rs.getString("object_identity");
            PermissionTargetInfo permissionTarget =
                    UpdateUtils.createFromObjectIdentity(objectIdentity);
            allAcls.put(id, new AclInfo(permissionTarget));
            return permissionTarget;
        }
    }

    private class AllAcls extends MappingSqlQuery {
        /*
        CREATE TABLE acl_permission (
             id BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,
             acl_object_identity BIGINT NOT NULL,
             recipient VARCHAR(250) NOT NULL,
             mask INTEGER NOT NULL,
             CONSTRAINT unique_recipient UNIQUE(acl_object_identity, recipient),
             FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity(id)
        );
        */

        private AllAcls() {
            super(getDataSource(), ALL_ACE_QUERY);
            compile();
        }

        @Override
        protected Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            long aclObjectId = rs.getLong("acl_object_identity");
            AclInfo aclInfo = allAcls.get(aclObjectId);
            AceInfo aceInfo = new AceInfo(rs.getString("recipient"), false, rs.getInt("mask"));
            UpdateUtils.updateAceMask(aceInfo);
            aclInfo.getAces().add(aceInfo);
            return aclInfo;
        }
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        // TODO: Send REST request to new Artifactory
    }

}
