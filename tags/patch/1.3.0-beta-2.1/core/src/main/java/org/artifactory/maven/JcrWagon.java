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
import org.apache.log4j.Logger;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.codehaus.plexus.util.IOUtil;

import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrWagon extends AbstractWagon {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrWagon.class);

    public void get(final String resName, final File dest)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        getIfNewer(resName, dest, -1);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public boolean getIfNewer(final String resName, final File dest, final long timestamp)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        ArtifactoryContext context = ContextHelper.get();
        CentralConfig cc = context.getCentralConfig();
        final LocalRepo repo = getRepo(cc);
        final Resource wagonRes = new Resource(resName);
        createParentDirectories(dest);
        JcrFsItem item;
        try {
            if (!repo.itemExists(resName)) {
                return false;
            } else {
                item = repo.getFsItem(resName);
                if (item.isDirectory()) {
                    throw new RepositoryException(
                            "Resource '" + resName + "' is a directory not a file.");
                }
                JcrFile file = ((JcrFile) item);
                long lastModified = file.getLastModified();
                if (lastModified > timestamp) {
                    wagonRes.setContentLength(file.getSize());
                    wagonRes.setLastModified(lastModified);
                    fireGetStarted(wagonRes, dest);
                    file.export(dest);
                    //Update the checksums
                    TransferEvent transferEvent =
                            new TransferEvent(JcrWagon.this, wagonRes,
                                    TransferEvent.TRANSFER_PROGRESS,
                                    TransferEvent.REQUEST_GET);
                    updateChecksums(transferEvent, dest);
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
            LOGGER.warn("Get failed.", e);
            throw tfe;
        }
    }

    public void put(File source, String dest)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        ArtifactoryContext context = ContextHelper.get();
        CentralConfig cc = context.getCentralConfig();
        final LocalRepo repo = getRepo(cc);
        Resource wagonRes = new Resource(dest);
        wagonRes.setContentLength(source.length());
        long lastModified = source.lastModified();
        wagonRes.setLastModified(lastModified);
        firePutStarted(wagonRes, source);
        SimpleRepoResource res = new SimpleRepoResource(repo, dest);
        res.setLastModified(lastModified);
        //Seperate the checksum calculation from the save operation, due to the fact that jackrabbit
        //reads the file with offsets more than a single time, so we cannot simply use the saved
        //stream to calculate the checksum as we save
        Resource resource = new Resource(dest);
        TransferEvent event = new TransferEvent(this, resource, TransferEvent.TRANSFER_PROGRESS,
                TransferEvent.REQUEST_PUT);
        updateChecksums(event, source);
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(source));
            repo.saveResource(res, is);
        } catch (IOException e) {
            fireTransferError(wagonRes, e, TransferEvent.REQUEST_PUT);
            TransferFailedException tfe =
                    new TransferFailedException("Could not save resource to '" + dest + "'.", e);
            //Log the exception since the deployer will swallow it
            LOGGER.warn("Put failed.", e);
            throw tfe;
        } finally {
            IOUtil.close(is);
        }
        firePutCompleted(wagonRes, source);
    }

    private void updateChecksums(TransferEvent event, File file) {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        TransferProgressReportingInputStream is = null;
        try {
            event.setLocalFile(file);
            is = new TransferProgressReportingInputStream(
                    file, this.getTransferEventSupport(), event);
            while (is.read(buffer) > 0) {
                //Nothing
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to transfer file.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private LocalRepo getRepo(CentralConfig cc)
            throws TransferFailedException, ResourceDoesNotExistException {
        String repoKey = getRepoKey();
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        final LocalRepo repo = virtualRepo.localRepositoryByKey(repoKey);
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

    public void closeConnection() throws ConnectionException {
    }
}
