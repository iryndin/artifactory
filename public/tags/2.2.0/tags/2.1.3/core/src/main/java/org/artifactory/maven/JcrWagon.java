/*
 * This file is part of Artifactory.
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
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;

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
            RepoResource res = repo.getInfo(new NullRequestContext(resName));
            if (!res.isFound()) {
                return false;
            } else {
                handle = repo.getResourceStreamHandle(res);
                long lastModified = res.getLastModified();
                if (lastModified > timestamp) {
                    wagonRes.setContentLength(handle.getSize());
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
                repo.saveResource(res, is, null);
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
