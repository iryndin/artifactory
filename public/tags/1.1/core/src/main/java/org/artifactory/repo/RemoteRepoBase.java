package org.artifactory.repo;

import org.apache.log4j.Logger;
import org.artifactory.cache.DefaultRetrievalCache;
import org.artifactory.cache.RetrievalCache;
import org.artifactory.engine.RepoAccessException;
import org.artifactory.engine.ResourceStreamHandle;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XmlType(name = "RemoteRepoBaseType", propOrder = {"url", "hardFail", "storeArtifactsLocally",
        "retrievalCachePeriodSecs", "cacheRetrievalFailures", "cacheRetrievalMisses"})
public abstract class RemoteRepoBase extends RepoBase implements RemoteRepo {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RemoteRepoBase.class);

    private String url;
    private boolean cacheRetrievalMisses = true;
    private boolean cacheRetrievalFailures = true;
    private boolean hardFail;
    private long retrievalCachePeriodSecs = 43200;//12h
    protected boolean storeArtifactsLocally = true;

    private LocalCacheRepo localCacheRepo;

    /**
     * A cache of retrieval misses and failures
     */
    private RetrievalCache badRetrievalsCache;


    public void init(CentralConfig cc) {
        //Initialize the local cache
        if (!isStoreArtifactsLocally()) {
            return;
        }
        localCacheRepo = new LocalCacheRepo(this, cc);
        //Same blackout and include/exclude settings for the cache
        localCacheRepo.setBlackedOut(isBlackedOut());
        localCacheRepo.setIncludesPattern(getIncludesPattern());
        localCacheRepo.setExcludesPattern(getExcludesPattern());
        //Create the retrieval cache
        //Multiply by 1000 because the config file is in seconds, but the cache is in milliseconds
        badRetrievalsCache = new DefaultRetrievalCache(retrievalCachePeriodSecs * 1000);
        if (getRetrievalCachePeriodSecs() > 0) {
            LOGGER.info(this + ": Enabling retrieval cache with period of "
                    + getRetrievalCachePeriodSecs() + " seconds");
        } else {
            LOGGER.info(this + ": Disabling retrieval cache");
            cacheRetrievalMisses = false;
            cacheRetrievalFailures = false;
        }
    }

    @XmlElement(required = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isLocal() {
        return false;
    }

    /**
     * if a repository is set to hard fail, then the download engine will terminate the whole
     * download process (with a status 500) if any of the repositories have unexpected errors.
     */
    @XmlElement(defaultValue = "false", required = false)
    public boolean isHardFail() {
        return hardFail;
    }

    @XmlElement(defaultValue = "3600", required = false)
    public long getRetrievalCachePeriodSecs() {
        return retrievalCachePeriodSecs;
    }

    /**
     * If a file repository is set to "store" mode, it will copy the found files into the main
     * repository store.
     */
    @XmlElement(defaultValue = "true", required = false)
    public boolean isStoreArtifactsLocally() {
        return storeArtifactsLocally;
    }

    @XmlElement(defaultValue = "true", required = false)
    public boolean isCacheRetrievalFailures() {
        return cacheRetrievalFailures;
    }

    public void setCacheRetrievalFailures(boolean cacheRetrievalFailures) {
        this.cacheRetrievalFailures = cacheRetrievalFailures;
    }

    @XmlElement(defaultValue = "true", required = false)
    public boolean isCacheRetrievalMisses() {
        return cacheRetrievalMisses;
    }

    public void setCacheRetrievalMisses(boolean cacheRetrievalMisses) {
        this.cacheRetrievalMisses = cacheRetrievalMisses;
    }

    public void setHardFail(boolean hardFail) {
        this.hardFail = hardFail;
    }

    public void setRetrievalCachePeriodSecs(long retrievalCachePeriodSecs) {
        this.retrievalCachePeriodSecs = retrievalCachePeriodSecs;
    }

    public void setStoreArtifactsLocally(boolean storeArtifactsLocally) {
        this.storeArtifactsLocally = storeArtifactsLocally;
    }

    /**
     * @param path
     * @return
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    public final RepoResource getInfo(String path) {
        //Skip if in blackout or not accepting
        if (isBlackedOut() || !accept(path)) {
            return new NotFoundRepoResource(path, this);
        }
        RepoResource res;
        try {
            synchronized (badRetrievalsCache) {
                /*
                //If update query and maven-metadata.xml is asked for, ignore the cache.
                if (!MavenUtil.isMetaData(path)) {
                */
                //Try to get it from the cache
                res = badRetrievalsCache.getResource(path);
                /*}*/
                if (res == null) {
                    //Try to get it from the remote repository
                    res = retrieveInfo(path);
                    if (!res.isFound()) {
                        //Update the non-found cache for a miss
                        LOGGER.info(this + ": " + res + " not found at '" + path + "'.");
                        if (cacheRetrievalMisses) {
                            badRetrievalsCache.setResource(res);
                        }
                    }
                } else if (!res.isFound()) {
                    LOGGER.info(this + ": " + res + " cahced as not found at '" + path + "'.");
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                this + ": " + res + " retrieved from cache at '" + path + "'.");
                    }
                }
            }
            return res;
        } catch (Exception e) {
            LOGGER.error(this + ": Error in getting information for '" + path +
                    "' (" + e.getMessage() + ").");
            //Update the non-found cache for a failure
            res = new NotFoundRepoResource(path, this);
            if (cacheRetrievalFailures) {
                badRetrievalsCache.setResource(res);
                //TODO: [by yl] If the remote repo is down and we failed in getting metadata
                //pom/artifacts will still be taken from the cache. Maybe we should clean up from
                //the cache all matching artifacts for the metadata url
            }
            if (isHardFail()) {
                throw new RepoAccessException(this, path, e.getLocalizedMessage(), e);
            }
            return res;
        }
    }

    public LocalCacheRepo getLocalCacheRepo() {
        return localCacheRepo;
    }

    public RepoResource removeFromRetrievalCache(String path) {
        return badRetrievalsCache.removeResource(path);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException {
        String relPath = res.getRelPath();
        if (storeArtifactsLocally) {
            LOGGER.info("Copying " + relPath + " from " + this + " to " + localCacheRepo);
            RepoResource targetRes = localCacheRepo.retrieveInfo(relPath);
            //Retrieve remotely only if locally cached artifact is older than remote one
            if (!targetRes.isFound() ||
                    res.getLastModified().after(targetRes.getLastModified())) {
                ResourceStreamHandle handle = retrieveResource(relPath);
                try {
                    //Create/override the resource in the storage cache
                    InputStream is = handle.getInputStream();
                    localCacheRepo.saveResource(res, is);
                } finally {
                    handle.close();
                }
            }
            //Update the resource retrieval cache
            badRetrievalsCache.removeResource(relPath);
            return localCacheRepo.getResourceStreamHandle(targetRes);
        } else {
            ResourceStreamHandle handle = retrieveResource(relPath);
            return handle;
        }
    }

    public void undeploy(String relPath) {
        //Update the repository caches
        if (isStoreArtifactsLocally()) {
            localCacheRepo.undeploy(relPath);
        }
        removeFromRetrievalCache(relPath);
        LOGGER.info("Removing " + relPath + " from " + localCacheRepo);
    }
}
