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
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.utils.MutableBoolean;
import org.codehaus.plexus.util.IOUtil;

import javax.jcr.RepositoryException;
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
        ArtifactoryContext context = ContextUtils.getContext();
        CentralConfig cc = context.getCentralConfig();
        final LocalRepo repo = getRepo(cc);
        final Resource wagonRes = new Resource(resName);
        JcrFsItem item;
        final MutableBoolean result = new MutableBoolean();
        createParentDirectories(dest);
        try {
            JcrHelper jcr = cc.getJcr();
            item = jcr.doInSession(
                    new JcrCallback<JcrFsItem>() {
                        public JcrFsItem doInJcr(JcrSessionWrapper session)
                                throws RepositoryException {
                            JcrFsItem item = repo.getFsItem(resName, session);
                            if (item == null) {
                                return null;
                            }
                            if (item.isDirectory()) {
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
                                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                                TransferEvent transferEvent =
                                        new TransferEvent(JcrWagon.this, wagonRes,
                                                TransferEvent.TRANSFER_PROGRESS,
                                                TransferEvent.REQUEST_GET);
                                InputStream is = null;
                                try {
                                    is = new FileInputStream(dest);
                                    while (true) {
                                        int n = is.read(buffer);
                                        if (n == -1) {
                                            break;
                                        }
                                        fireTransferProgress(transferEvent, buffer, n);
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to transfer file.", e);
                                } finally {
                                    IOUtils.closeQuietly(is);
                                }
                                fireGetCompleted(wagonRes, dest);
                                result.setValue(true);
                                return item;
                            } else {
                                return null;
                            }
                        }
                    }
            );
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
        ArtifactoryContext context = ContextUtils.getContext();
        CentralConfig cc = context.getCentralConfig();
        final LocalRepo repo = getRepo(cc);
        Resource wagonRes = new Resource(dest);
        wagonRes.setContentLength(source.length());
        long lastModified = source.lastModified();
        wagonRes.setLastModified(lastModified);
        firePutStarted(wagonRes, source);
        SimpleRepoResource res = new SimpleRepoResource(repo, dest);
        res.setLastModifiedTime(lastModified);
        TransferProgressReportingInputStream is = null;
        try {
            //Needed to update the digest
            Resource resource = new Resource(dest);
            is = new TransferProgressReportingInputStream(
                    source, resource, this, this.getTransferEventSupport());
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

    private LocalRepo getRepo(CentralConfig cc)
            throws TransferFailedException, ResourceDoesNotExistException {
        String repoKey = getRepoKey();
        final LocalRepo repo = cc.localRepositoryByKey(repoKey);
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
