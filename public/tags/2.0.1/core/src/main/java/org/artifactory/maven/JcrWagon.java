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
package org.artifactory.maven;

import org.apache.commons.io.IOUtils;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is called when deploying from the web ui. The JcrWagon simple saves the uploaded file to Jcr. It relies on
 * the wagon implementation to do all the deployment calculations (snapshot version, metadata etc.)
 *
 * @author yoavl
 */
public class JcrWagon extends AbstractWagon {
    private static final Logger log = LoggerFactory.getLogger(JcrWagon.class);

    public void get(final String resName, final File dest)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        getIfNewer(resName, dest, -1);
    }

    public boolean getIfNewer(final String resName, final File dest, final long timestamp)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (NamingUtils.isChecksum(resName)) {
            //DO not answer checksums
            return false;
        }
        final LocalRepo repo = getRepo();
        final Resource wagonRes = new Resource(resName);
        createParentDirectories(dest);
        ResourceStreamHandle handle = null;
        FileOutputStream fileOutputStream = null;
        try {
            RepoResource res = repo.getInfo(resName);
            if (!res.isFound()) {
                return false;
            } else {
                handle = repo.getResourceStreamHandle(res);
                long lastModified = res.getLastModified();
                if (lastModified > timestamp) {
                    wagonRes.setContentLength(res.getSize());
                    wagonRes.setLastModified(lastModified);
                    fireGetStarted(wagonRes, dest);
                    //Do the actual resource transfer into the temp file
                    fileOutputStream = new FileOutputStream(dest);
                    IOUtils.copy(handle.getInputStream(), fileOutputStream);
                    //Update the checksums
                    TransferEvent transferEvent =
                            new TransferEvent(JcrWagon.this, wagonRes,
                                    TransferEvent.TRANSFER_PROGRESS,
                                    TransferEvent.REQUEST_GET);
                    updateWagonChecksums(transferEvent, dest);
                    fireGetCompleted(wagonRes, dest);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            fireTransferError(wagonRes, e, TransferEvent.REQUEST_GET);
            TransferFailedException tfe =
                    new TransferFailedException("Could not read from '" + resName + "'.", e);
            //Log the exception since the deployer will swallow it
            log.warn("Get failed.", e);
            throw tfe;
        } finally {
            if (handle != null) {
                handle.close();
            }
            IOUtils.closeQuietly(fileOutputStream);
        }
    }

    public void put(File source, String dest)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource wagonRes = doSomeWagonStuff(source, dest);
        // now create the JcrFile and related info
        long lastModified = source.lastModified();
        wagonRes.setLastModified(lastModified);
        firePutStarted(wagonRes, source);
        if (!NamingUtils.isChecksum(dest)) {
            LocalRepo repo = getRepo();
            RepoPath repoPath = new RepoPath(repo.getKey(), dest);
            RepoResource res;
            if (NamingUtils.isMetadata(dest)) {
                res = new MetadataResource(new MetadataInfo(repoPath));
            } else {
                res = new FileResource(repoPath);
                // create trusted original file checksums
                FileInfo fileInfo = (FileInfo) res.getInfo();
                fileInfo.createTrustedChecksums();
                fileInfo.setLastModified(lastModified);
            }
            //Seperate the checksum calculation from the save operation, due to the fact that jackrabbit
            //reads the file with offsets more than a single time, so we cannot simply use the saved
            //stream to calculate the checksum as we save
            Resource resource = new Resource(dest);
            TransferEvent event =
                    new TransferEvent(this, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT);
            updateWagonChecksums(event, source);
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(source));
                repo.saveResource(res, is);
            } catch (IOException e) {
                fireTransferError(wagonRes, e, TransferEvent.REQUEST_PUT);
                TransferFailedException tfe =
                        new TransferFailedException("Could not save resource to '" + dest + "'.", e);
                //Log the exception since the deployer will swallow it
                log.warn("Put failed.", e);
                throw tfe;
            } finally {
                IOUtil.close(is);
            }
        }
        // tell all listeners we are finished
        firePutCompleted(wagonRes, source);
    }

    /**
     * This method will cause the creation of checksum files that are later used by the wagon manager. We do it even
     * though we don't use the generated checksums because the manager will fail otherwise. We WILL remove this code in
     * the future.
     */
    private Resource doSomeWagonStuff(File source, String dest) {
        Resource wagonRes = new Resource(dest);
        wagonRes.setContentLength(source.length());
        wagonRes.setLastModified(source.lastModified());
        firePutStarted(wagonRes, source);
        //Seperate the checksum calculation from the save operation, due to the fact that jackrabbit
        //reads the file with offsets more than a single time, so we cannot simply use the saved
        //stream to calculate the checksum as we save
        Resource resource = new Resource(dest);
        TransferEvent event = new TransferEvent(
                this, resource, TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT);
        updateWagonChecksums(event, source);
        return wagonRes;
    }

    private void updateWagonChecksums(TransferEvent event, File file) {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        TransferProgressReportingInputStream is = null;
        try {
            event.setLocalFile(file);
            is = new TransferProgressReportingInputStream(file, this.getTransferEventSupport(), event);
            while (is.read(buffer) > 0) {
                //Nothing
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to transfer file.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private LocalRepo getRepo() throws TransferFailedException, ResourceDoesNotExistException {
        String repoKey = getRepoKey();
        ArtifactoryContext context = ContextHelper.get();
        InternalRepositoryService repositoryService = (InternalRepositoryService) context.getRepositoryService();
        final LocalRepo repo = repositoryService.localRepositoryByKey(repoKey);
        if (repo == null) {
            throw new ResourceDoesNotExistException("Repo '" + repoKey + "' does not exist.");
        }
        return repo;
    }

    private String getRepoKey() throws TransferFailedException {
        String repoKey = getRepository().getId();
        if (repoKey == null) {
            throw new TransferFailedException("Unable to operate with a null repo.");
        }
        return repoKey;
    }

    public void openConnection() throws ConnectionException, AuthenticationException {
    }

    @Override
    public void closeConnection() throws ConnectionException {
    }
}
