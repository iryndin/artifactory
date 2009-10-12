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

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.JcrPath;
import org.artifactory.repo.LocalRepo;
import org.sonatype.nexus.artifact.Gav;

import java.io.File;

/**
 * @author freds
 * @date Oct 24, 2008
 */
public final class ExtensionBasedLocator extends ArtifactoryLocator {
    private static final String POM_EXTENSION = "." + ContentType.mavenPom.getDefaultExtension();
    private final String expectedExtension;

    public ExtensionBasedLocator(LocalRepo localRepo, String expectedExtension) {
        super(localRepo);
        this.expectedExtension = expectedExtension;
    }

    public File locate(File source, Gav gav) {
        RepoPath repoPath = JcrPath.get().getRepoPath(source.getAbsolutePath());
        String name = source.getName();
        if (name.endsWith(expectedExtension)) {
            return getLocalRepo().getJcrFile(repoPath);
        }
        if (name.endsWith(POM_EXTENSION)) {
            String relPath = repoPath.getPath();
            relPath = relPath.substring(0, relPath.length() - POM_EXTENSION.length()) +
                    expectedExtension;
            repoPath = new RepoPath(repoPath.getRepoKey(), relPath);
            return getLocalRepo().getJcrFile(repoPath);
        }
        return null;
    }
}
