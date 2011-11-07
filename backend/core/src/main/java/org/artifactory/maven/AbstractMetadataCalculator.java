/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.mime.MavenNaming;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

/**
 * @author freds
 */
public class AbstractMetadataCalculator {
    private static final Logger log = LoggerFactory.getLogger(AbstractMetadataCalculator.class);

    private JcrService jcrService;
    private InternalRepositoryService repositoryService;

    protected InternalRepositoryService getRepositoryService() {
        if (repositoryService == null) {
            repositoryService = (InternalRepositoryService) InternalContextHelper.get().getRepositoryService();
        }
        return repositoryService;
    }

    protected JcrService getJcrService() {
        if (jcrService == null) {
            jcrService = InternalContextHelper.get().getJcrService();
        }
        return jcrService;
    }

    protected void saveMetadata(RepoPath repoPath, Metadata metadata, BasicStatusHolder status) {
        String metadataStr;
        try {
            metadataStr = MavenModelUtils.mavenMetadataToString(metadata);
            // Write lock auto upgrade supported LockingHelper.releaseReadLock(repoPath);
            getRepositoryService().setXmlMetadataLater(repoPath, MavenNaming.MAVEN_METADATA_NAME, metadataStr);
        } catch (Exception e) {
            status.setError("Error while writing metadata for " + repoPath + ".", e, log);
        }
    }
}
