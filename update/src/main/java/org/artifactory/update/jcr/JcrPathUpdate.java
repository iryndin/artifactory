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
package org.artifactory.update.jcr;

import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.update.ArtifactoryVersion;
import org.artifactory.update.VersionsHolder;
import org.artifactory.update.config.ArtifactoryConfigVersion;
import org.artifactory.update.utils.UpdateUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: freds Date: Jun 5, 2008 Time: 12:40:44 AM
 */
public abstract class JcrPathUpdate {
    public static final Map<String, String> reverseSubstituteRepoKeys =
            new HashMap<String, String>();

    protected static final String REPO_ROOT = "repositories";
    protected static final String NODE_CONFIGURATION = "configuration";
    private static ArtifactoryVersion version;

    private static ArtifactoryVersion getVersion() {
        if (version == null) {
            version = VersionsHolder.getOriginalVersion();
        }
        return version;
    }

    public static void setVersion(ArtifactoryVersion version) {
        JcrPathUpdate.version = version;
    }

    public static String getOcmJcrRootPath() {
        return "/" + NODE_CONFIGURATION;
    }

    public static String getOcmClassJcrPath(String classKey) {
        return "/" + NODE_CONFIGURATION + "/" + classKey;
    }

    public static String getRepoJcrRootPath() {
        // The REPO_ROOT was added at revision 892
        if (getVersion().getRevision() >= 892) {
            return "/" + REPO_ROOT + "/";
        }
        return "/";
    }

    /**
     * @return A path relative to the current repoKey. For example: /repositories/repo-key/org/jfrog
     *         will return org/jfrog
     */
    public static String getNodeRelativePath(Node node, String repoKey) throws RepositoryException {
        String repositoriesRootPath = getRepoJcrRootPath();
        String currentRepoRootPath = repositoriesRootPath + repoKey;
        String absolutePath = node.getPath();
        if (absolutePath.equals(currentRepoRootPath)) {
            // current folder is the root of the repository
            return "";
        } else {
            return absolutePath.replaceFirst(currentRepoRootPath + "/", "");
        }
    }

    public static File getRepoExportDir(File exportDir, String repoKey) {
        String newRepoKey = UpdateUtils.getNewRepoKey(repoKey);
        File repositoriesBaseDir = new File(exportDir, REPO_ROOT);
        return new File(repositoriesBaseDir, newRepoKey);
    }

    public static String getRepoJcrPath(String repoKey) {
        String inJcrRepoKey = repoKey;

        // If config schema is 1.0 the key may be changed by -Dsubst.
        if (repoKeyMightSubstituted()) {
            if (reverseSubstituteRepoKeys.isEmpty()) {
                Set<Map.Entry<String, String>> entries =
                        ArtifactoryConstants.substituteRepoKeys.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    reverseSubstituteRepoKeys.put(entry.getValue(), entry.getKey());
                }
            }
            inJcrRepoKey = reverseSubstituteRepoKeys.get(repoKey);
            if (inJcrRepoKey == null) {
                inJcrRepoKey = repoKey;
            }
        }
        // The REPO_ROOT was added at revision 892
        if (getVersion().getRevision() >= 892) {
            return "/" + REPO_ROOT + "/" + inJcrRepoKey;
        }
        return "/" + inJcrRepoKey;
    }

    private static boolean repoKeyMightSubstituted() {
        return (ArtifactoryConfigVersion.OneOne.getUntilArtifactoryVersion().getRevision() >=
                getVersion().getRevision())
                && !ArtifactoryConstants.substituteRepoKeys.isEmpty();
    }
}
