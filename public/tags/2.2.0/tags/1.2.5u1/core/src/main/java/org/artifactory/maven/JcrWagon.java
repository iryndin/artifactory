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
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.MutableBoolean;
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
        final MutableBoolean result = new MutableBoolean();
        createParentDirectories(dest);
        JcrFsItem item;
        try {
            if (!repo.itemExists(resName)) {
                item = null;
            } else {
                item = repo.getFsItem(resName);
                if (item.isFolder()) {
                    throw new RepositoryException(
                            "Resource '" + resName + "' is a directory not a file.");
                }
                JcrFile file = ((JcrFile) item);
                long lastModified = file.lastModifiedTime();
                if (lastModified > timestamp) {
                    wagonRes.setContentLength(file.size());
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
                    result.setValue(true);
                }
            }
        } catch (Exception e) {
            fireTransferError(wagonRes, e, TransferEvent.REQUEST_GET);
            TransferFailedException tfe =
                    new TransferFailedException("Could not read from '" + resName + "'.", e);
            //Log the exception since the deployer will swallow it
            LOGGER.warn("Get failed.", e);
            throw tfe;
        }
        if (item == null) {
            //Resource not found
            throw new TransferFailedException(
                    getRepository().getUrl() + " - Could find resource: '" + resName + "'.");
        } else {
            return result.value();
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
        res.setLastModifiedTime(lastModified);
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
            repo.saveResource(res, is, false);
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
