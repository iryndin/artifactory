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
package org.artifactory.jcr;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.utils.PathUtils;

import java.io.File;

/**
 * This singleton is reposible for providing the path structure in the JCR DB, and for converting
 * RepoPath to absPath (in JCR) and vice-versa. User: freds Date: Jun 4, 2008 Time: 11:36:11 PM
 */
public class JcrPath {

    protected static final String REPO_ROOT = "repositories";
    protected static final String NODE_CONFIGURATION = "configuration";

    /**
     * Strange stuff of a child class overidable singleton
     */
    private static JcrPath instance = new JcrPath();

    protected JcrPath() {
    }

    public static JcrPath get() {
        return instance;
    }

    protected static void set(JcrPath jcrPath) {
        JcrPath.instance = jcrPath;
    }

    public String getRepoJcrRootPath() {
        return "/" + REPO_ROOT;
    }

    public String getOcmJcrRootPath() {
        return "/" + NODE_CONFIGURATION;
    }

    public String getRepoJcrPath(String repoKey) {
        return "/" + REPO_ROOT + "/" + repoKey;
    }

    public String getOcmClassJcrPath(String classKey) {
        return "/" + NODE_CONFIGURATION + "/" + classKey;
    }

    public File getRepoExportDir(File exportDir, String repoKey) {
        return new File(new File(exportDir, REPO_ROOT), repoKey);
    }

    public String getAbsolutePath(RepoPath repoPath) {
        String key = repoPath.getRepoKey();
        String relPath = repoPath.getPath();
        String absPath = getRepoJcrPath(key) + (relPath.length() > 0 ? "/" + relPath : "");
        return absPath;
    }

    public RepoPath getRepoPath(String absPath) {
        String modifiedAbsPath = absPath.replace('\\', '/');
        String repoJcrRootPath = getRepoJcrRootPath();
        String repoKey = repoKeyFromPath(modifiedAbsPath);
        String relPath = PathUtils.formatRelativePath(
                modifiedAbsPath.substring(repoJcrRootPath.length() + repoKey.length() + 1));
        RepoPath repoPath = new RepoPath(repoKey, relPath);
        return repoPath;
    }

    private String repoKeyFromPath(String absPath) {
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
        int idx = absPath.indexOf(repoJcrRootPath);
        if (idx == -1) {
            throw new IllegalArgumentException("Path '" + absPath + "' is not a repository path.");
        }
        int repoKeyEnd = absPath.indexOf("/", repoJcrRootPath.length() + 1);
        int repoKeyBegin = repoJcrRootPath.length() + 1;
        String repoKey = repoKeyEnd > 0 ? absPath.substring(repoKeyBegin, repoKeyEnd) :
                absPath.substring(repoKeyBegin);
        return repoKey;
    }
}
