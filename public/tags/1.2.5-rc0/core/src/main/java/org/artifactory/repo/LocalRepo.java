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
package org.artifactory.repo;

import org.apache.maven.model.Model;
import org.artifactory.config.ExportableConfig;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface LocalRepo extends Repo, ExportableConfig {
    String REPO_ROOT = "repositories";

    JcrFsItem getFsItem(String relPath, Session session);

    SnapshotVersionBehavior getSnapshotVersionBehavior();

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    Model getModel(ArtifactResource pa);

    String getPomContent(ArtifactResource pa);

    void saveResource(RepoResource res, InputStream stream, boolean createChecksum)
            throws IOException;

    Node getRepoJcrNode(Session session);

    void importFromDir(File dir, boolean singleTransation);

    void exportToDir(File dir);

    void undeploy(String path);

    String getRepoPath();

    void delete();
}