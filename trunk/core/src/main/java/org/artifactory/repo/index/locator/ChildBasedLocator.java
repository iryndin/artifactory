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
package org.artifactory.repo.index.locator;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.repo.LocalRepo;
import org.sonatype.nexus.artifact.Gav;

import java.io.File;

/**
 * @author freds
 * @date Oct 24, 2008
 */
public abstract class ChildBasedLocator extends ArtifactoryLocator {
    public ChildBasedLocator(LocalRepo localRepo) {
        super(localRepo);
    }

    public File locate(File source, Gav gav) {
        RepoPath parentRepoPath = JcrPath.get().getRepoPath(source.getParent());
        String fileName = getChildName(source, gav);
        RepoPath repoPath = new RepoPath(parentRepoPath, fileName);
        JcrFile jcrFile = getLocalRepo().getJcrFile(repoPath);
        if (jcrFile == null) {
            //Should never return null to the sensitive indexer - return a non-exiting file
            return new File(JcrPath.get().getAbsolutePath(repoPath)) {
                @Override
                public boolean exists() {
                    return false;
                }
            };
        }
        return jcrFile;
    }

    protected abstract String getChildName(File source, Gav gav);
}
