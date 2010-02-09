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
package org.artifactory.update;

import org.artifactory.ArtifactoryConstants;
import org.artifactory.jcr.JcrPath;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: freds Date: Jun 5, 2008 Time: 12:40:44 AM
 */
public class JcrPathUpdate extends JcrPath {
    public static final Map<String, String> reverseSubstituteRepoKeys =
            new HashMap<String, String>();

    private final ArtifactoryVersion version;

    public static void register(ArtifactoryVersion version) {
        new JcrPathUpdate(version);
    }

    protected JcrPathUpdate(ArtifactoryVersion version) {
        this.version = version;
        set(this);
    }

    public String getRepoJcrRootPath() {
        // The REPO_ROOT was added at revision 892
        if (version.getRevision() >= 892) {
            return super.getRepoJcrRootPath();
        }
        return "/";
    }

    public File getRepoExportDir(File exportDir, String repoKey) {
        // When repo root is empty the repo key will be added by JcrFolder
        if (version.getRevision() >= 892) {
            return super.getRepoExportDir(exportDir, repoKey);
        }
        return new File(exportDir, REPO_ROOT);
    }

    public String getRepoJcrPath(String repoKey) {
        String inJcrRepoKey = repoKey;

        // If config schema is 1.0 the key may be changed by -Dsubst.
        if ((ArtifactoryConfigVersion.OneOne.getUntilArtifactoryVersion().getRevision() >=
                version.getRevision())
                && !ArtifactoryConstants.substituteRepoKeys.isEmpty()) {
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
        if (version.getRevision() >= 892) {
            return super.getRepoJcrPath(inJcrRepoKey);
        }
        return "/" + inJcrRepoKey;
    }
}
