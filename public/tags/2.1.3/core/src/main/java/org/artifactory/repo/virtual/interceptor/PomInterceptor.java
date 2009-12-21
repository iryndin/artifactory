/*
 * Copyright 2009 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.virtual.interceptor;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepoAccessException;
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
            log.error(message, e);
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
                    transformedResource, IOUtils.toInputStream(pomContent), null);
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
                pomAsString = IOUtils.toString(inputStream);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        PomCleanupPolicy cleanupPolicy = virtualRepo.getPomRepositoryReferencesCleanupPolicy();
        PomTransformer transformer = new PomTransformer(pomAsString, cleanupPolicy);
        return transformer.transform();
    }
}