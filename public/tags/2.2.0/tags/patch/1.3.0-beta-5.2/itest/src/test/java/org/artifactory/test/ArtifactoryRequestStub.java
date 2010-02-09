package org.artifactory.test;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequestBase;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yossi Shaul
 */
class ArtifactoryRequestStub extends ArtifactoryRequestBase {

    private String uri;
    private InputStream inputStream;

    public ArtifactoryRequestStub(String path) {
        this("repo", path);
    }

    public ArtifactoryRequestStub(String repoKey, String path) {
        setRepoPath(new RepoPath(repoKey, path));
        uri = "localhost:8080/" + repoKey + "/" + path;
    }

    public long getLastModified() {
        return 0;
    }

    public long getIfModifiedSince() {
        return 0;
    }

    public String getSourceDescription() {
        return "SourceDescription";
    }

    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    public boolean isHeadOnly() {
        return false;
    }

    public boolean isRecursive() {
        return false;
    }

    public boolean isFromAnotherArtifactory() {
        return false;
    }

    public int getContentLength() {
        return 0;
    }

    public String getHeader(String headerName) {
        return null;
    }

    public boolean isWebdav() {
        return false;
    }

    public String getUri() {
        return uri;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}
