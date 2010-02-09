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

package org.artifactory.repo.virtual.interceptor;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.Repo;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.repo.virtual.interceptor.transformer.PomTransformer;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Intercepts pom resources, transforms the pom according to the policy and saves it to the local storage.
 *
 * @author Eli Givoni
 */
@Component
public class PomInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PomInterceptor.class);

    @Autowired
    private InternalRepositoryService repoService;

    public RepoResource onBeforeReturn(VirtualRepo virtualRepo, RequestContext context, RepoResource resource) {
        String resourcePath = resource.getResponseRepoPath().getPath();
        if (!"pom".equals(PathUtils.getExtension(resourcePath))) {
            return resource;
        }

        PomCleanupPolicy cleanupPolicy = virtualRepo.getPomRepositoryReferencesCleanupPolicy();
        if (cleanupPolicy.equals(PomCleanupPolicy.nothing)) {
            return resource;
        }

        String pomContent;
        try {
            pomContent = transformPomResource(resource, virtualRepo);
        } catch (IOException e) {
            String message = "Failed to transform pom file";
            if (ExceptionUtils.getRootCause(e) instanceof BadPomException) {
                log.error(message + ":" + e.getMessage());
            } else {
                log.error(message, e);
            }
            return new UnfoundRepoResource(resource.getRepoPath(), message + ": " + e.getMessage());
        }

        RepoPath localStoragePath = new RepoPath(virtualRepo.getKey(), resourcePath);
        FileInfoImpl fileInfo = new FileInfoImpl(localStoragePath);
        long now = System.currentTimeMillis();
        fileInfo.setCreated(now);
        fileInfo.setLastModified(now);
        fileInfo.createTrustedChecksums();
        fileInfo.setSize(pomContent.length());
        RepoResource transformedResource = new FileResource(fileInfo);

        try {
            transformedResource = virtualRepo.saveResource(
                    transformedResource, IOUtils.toInputStream(pomContent, "utf-8"), null);
        } catch (IOException e) {
            String message = "Failed to import file to local storage";
            log.error(message, e);
            return new UnfoundRepoResource(resource.getRepoPath(), message + ": " + e.getMessage());
        }

        return transformedResource;
    }

    private String transformPomResource(RepoResource resource, VirtualRepo virtualRepo) throws IOException {
        String repoKey = resource.getResponseRepoPath().getRepoKey();
        Repo repository = repoService.repositoryByKey(repoKey);
        ResourceStreamHandle handle;
        try {
            handle = repoService.getResourceStreamHandle(repository, resource);
        } catch (RepoAccessException e) {
            throw new IOException(e.getMessage());
        }

        InputStream inputStream = handle.getInputStream();
        String pomAsString = "";
        if (inputStream != null) {
            try {
                pomAsString = IOUtils.toString(inputStream, "utf-8");
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        PomCleanupPolicy cleanupPolicy = virtualRepo.getPomRepositoryReferencesCleanupPolicy();
        PomTransformer transformer = new PomTransformer(pomAsString, cleanupPolicy);
        return transformer.transform();
    }
}