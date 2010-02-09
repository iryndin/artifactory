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

import java.io.File;

/**
 * User: freds Date: Jun 4, 2008 Time: 11:36:11 PM
 */
@SuppressWarnings({"MethodMayBeStatic"})
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
}
