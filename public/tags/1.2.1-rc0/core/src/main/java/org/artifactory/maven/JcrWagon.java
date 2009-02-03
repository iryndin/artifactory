package org.artifactory.maven;

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

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
        ArtifactoryContext context = ContextUtils.getArtifactoryContext();
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
        ArtifactoryContext context = ContextUtils.getArtifactoryContext();
        CentralConfig cc = context.getCentralConfig();
        final LocalRepo repo = getRepo(cc);
        Resource wagonRes = new Resource(dest);
        wagonRes.setContentLength(source.length());
        long lastModified = source.lastModified();
        wagonRes.setLastModified(lastModified);
        firePutStarted(wagonRes, source);
        SimpleRepoResource res = new SimpleRepoResource(dest, repo);
        res.setLastModifiedTime(lastModified);
        try {
            FileInputStream fis = new FileInputStream(source);
            repo.saveResource(res, fis);
        } catch (IOException e) {
            fireTransferError(wagonRes, e, TransferEvent.REQUEST_PUT);
            TransferFailedException tfe =
                    new TransferFailedException("Could not save resource to '" + dest + "'.", e);
            //Log the exception since the deployer will swallow it
            LOGGER.warn("Put failed.", e);
            throw tfe;
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
