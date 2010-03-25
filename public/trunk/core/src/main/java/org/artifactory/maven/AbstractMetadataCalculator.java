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

package org.artifactory.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.spring.InternalContextHelper;

import java.io.IOException;

/**
 * @author freds
 */
public class AbstractMetadataCalculator {
    private JcrService jcrService;
    private RepositoryService repositoryService;

    protected RepositoryService getRepositoryService() {
        if (repositoryService == null) {
            repositoryService = InternalContextHelper.get().getRepositoryService();
        }
        return repositoryService;
    }

    protected JcrService getJcrService() {
        if (jcrService == null) {
            jcrService = InternalContextHelper.get().getJcrService();
        }
        return jcrService;
    }

    protected void saveMetadata(JcrFolder folder, Metadata metadata) {
        String metadataStr;
        try {
            metadataStr = MavenModelUtils.mavenMetadataToString(metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert maven metadata into string", e);
        }
        RepoPath repoPath = folder.getRepoPath();
        // Write lock auto upgrade supported LockingHelper.releaseReadLock(repoPath);
        getRepositoryService().setXmlMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME, metadataStr);
    }
}
