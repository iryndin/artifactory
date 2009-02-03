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
package org.artifactory.update.utils;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.update.VersionsHolder;
import org.artifactory.update.config.ConfigExporter;
import org.artifactory.update.v122rc0.JcrExporter;
import org.artifactory.version.ArtifactoryVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: freds Date: Aug 14, 2008 Time: 10:08:41 PM
 */
public class UpdateUtils {
    public static final String LOCAL_SUFFIX = "-local";
    public static final String CACHE_SUFFIX = "-cache";
    public static final char REPO_PATH_SEP = ':';
    public static final String ANY = "ANY";
    public static final String SECURITY_FILE_NAME = "security.xml";

    public static void initArtifactoryHome(File artifactoryHome) {
        ArtifactoryHome.setHomeDir(artifactoryHome);
        ArtifactoryHome.setReadOnly(true);
        ArtifactoryHome.create();
        ArtifactoryVersion version = VersionsHolder.getOriginalVersion();
        // At rev 1291 the root jcr folder moved up
        if (version.getRevision() < 1291) {
            File jcrRootDir;
            try {
                jcrRootDir = ArtifactoryHome.getOrCreateSubDir(
                        ArtifactoryHome.getDataDir(), "jcr");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ArtifactoryHome.setJcrRootDir(jcrRootDir);
        }
    }

    public static PermissionTargetInfo createFromObjectIdentity(String objectIdentity) {
        Assert.notNull(objectIdentity, "RepoAndGroupIdIdentity cannot have a null id");
        int idx = objectIdentity.indexOf(REPO_PATH_SEP);
        Assert.state(idx > 0, "Could not determine both repository key and groupId from '" +
                objectIdentity + "'.");
        String repoKey = objectIdentity.substring(0, idx);
        // get the new repository name from the original
        repoKey = UpdateUtils.getNewRepoKey(repoKey);
        String path = objectIdentity.substring(idx + 1);
        //Trim leading '/' (casued by webdav requests)
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        //Replace permissions: repo:ANY -> repo:**, repo:x/y->repo/x/y/**
        PermissionTargetInfo permissionTarget = null;
        if ("ANY".equals(path)) {
            path = PermissionTargetInfo.ANY_PATH;
            // If repoKey is ANY the name is now Anything
            if (repoKey.equals("ANY")) {
                permissionTarget = new PermissionTargetInfo(
                        PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME,
                        PermissionTargetInfo.ANY_REPO, PermissionTargetInfo.ANY_PATH, null
                );
            }
        } else {
            path = path.replace('.', '/') + "/" + PermissionTargetInfo.ANY_PATH;
        }
        if (permissionTarget == null) {
            permissionTarget = new PermissionTargetInfo(objectIdentity, repoKey, path, null);
        }
        return permissionTarget;
    }

    public static File exportSecurityData(File exportDir, List<UserInfo> users,
            List<AclInfo> acls) {
        OutputStream os = null;
        File path = null;
        try {
            path = new File(exportDir, SECURITY_FILE_NAME);
            File parentFile = path.getParentFile();
            FileUtils.forceMkdir(parentFile);
            SecurityInfo descriptor = new SecurityInfo(users,
                    new ArrayList<GroupInfo>(), acls);
            XStream xstream = getXstream(SecurityInfo.class);
            os = new BufferedOutputStream(new FileOutputStream(path));
            xstream.toXML(descriptor, os);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export security configuration.", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
        return path;
    }

    public static XStream getXstream(Class<SecurityInfo> type) {
        XStream xstream = new XStream();
        xstream.processAnnotations(type);
        return xstream;
    }

    public static AbstractApplicationContext getSpringContext() {
        ArtifactoryVersion version = VersionsHolder.getOriginalVersion();
        String[] springFiles = new String[3];
        springFiles[0] = version.findResource("updateApplicationContext.xml").toExternalForm();
        springFiles[1] = version.findResource("updateJcr.xml").toExternalForm();
        springFiles[2] = version.findResource("updateSecurity.xml").toExternalForm();
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(springFiles);
        return ctx;
    }

    public static ImportableExportable getSecurityExporter(ApplicationContext ctx) {
        return (ImportableExportable) ctx.getBean("securityExporter");
    }

    public static ConfigExporter getCentralConfigExporter(ApplicationContext ctx) {
        return (ConfigExporter) ctx.getBean("configExporter");
    }

    public static JcrExporter getJcrExporter(ApplicationContext ctx) {
        return (JcrExporter) ctx.getBean("jcrExporter");
    }

    /**
     * @param repoKey The repo key from the jcr database.
     * @return The new repo key after the keys substitution. Might be the same if no substitution
     *         occurred. Example of keys substitutions is from 3rd-party-lib to third-party-lib
     */
    public static String getNewRepoKey(String repoKey) {
        // If in substitute repo get new substituted name
        String newRepoKey = ArtifactoryConstants.substituteRepoKeys.get(repoKey);
        if (newRepoKey == null) {
            newRepoKey = repoKey;
        }

        // If it is local repository add the -local
        if (!newRepoKey.endsWith(CACHE_SUFFIX) && !newRepoKey.endsWith(LOCAL_SUFFIX)) {
            newRepoKey = newRepoKey + LOCAL_SUFFIX;
        }

        return newRepoKey;
    }

    /**
     * The delete permission was added in version 1.3.0-beta3. Grand the delete permission to
     * deployers and admins if importing from a version up to 1.3.0-beta2.
     */
    public static void updateAceMask(AceInfo aceInfo) {
        ArtifactoryVersion version = VersionsHolder.getOriginalVersion();
        if (version.before(ArtifactoryVersion.v130beta3)) {
            if (aceInfo.canAdmin() || aceInfo.canDeploy()) {
                aceInfo.setDelete(true);
            }
        }
    }
}
