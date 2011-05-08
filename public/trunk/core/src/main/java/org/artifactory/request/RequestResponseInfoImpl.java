package org.artifactory.request;

import com.google.common.collect.Sets;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * @author Yoav Landman
 */
public class RequestResponseInfoImpl implements MutableRequestResponseInfo {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(RequestResponseInfoImpl.class);

    private RepoResource repoResource;
    private RequestContext requestContext;
    private String remoteRepoUrl;

    public RequestResponseInfoImpl(RequestContext requestContext, RepoResource repoResource, String remoteRepoUrl) {
        this.requestContext = requestContext;
        this.repoResource = repoResource;
        this.remoteRepoUrl = remoteRepoUrl;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public RepoPath getRequestRepoPath() {
        return repoResource.getRepoPath();
    }

    public RepoPath getResponseRepoPath() {
        return repoResource.getResponseRepoPath();
    }

    public void setResponseRepoPath(RepoPath responsePath) {
        repoResource.setResponseRepoPath(responsePath);
    }

    @Nullable
    public String getRemoteRepoUrl() {
        return remoteRepoUrl;
    }

    public boolean isFound() {
        return repoResource.isFound();
    }

    public boolean isExactQueryMatch() {
        return repoResource.isExactQueryMatch();
    }

    public boolean isExpired() {
        return repoResource.isExpired();
    }

    public boolean isMetadata() {
        return repoResource.isMetadata();
    }

    public long getCacheAge() {
        return repoResource.getCacheAge();
    }

    public String getMimeType() {
        return repoResource.getMimeType();
    }

    public String getName() {
        return repoResource.getInfo().getName();
    }

    public long getLastModified() {
        return repoResource.getInfo().getLastModified();
    }

    public void setLastModified(long modified) {
        repoResource.getInfo().setLastModified(modified);
    }

    public long getSize() {
        return repoResource.getInfo().getSize();
    }

    public void setSize(long size) {
        repoResource.getInfo().setSize(size);
    }

    public String getSha1() {
        return repoResource.getInfo().getSha1();
    }

    public String getMd5() {
        return repoResource.getInfo().getMd5();
    }

    public void setSha1(String sha1) {
        repoResource.getInfo().setChecksums(Sets.newHashSet(new ChecksumInfo(ChecksumType.sha1, sha1, sha1)));
    }

    public void setMd5(String md5) {
        repoResource.getInfo().setChecksums(Sets.newHashSet(new ChecksumInfo(ChecksumType.md5, md5, md5)));
    }
}
