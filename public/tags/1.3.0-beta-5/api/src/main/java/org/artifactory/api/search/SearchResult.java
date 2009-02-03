package org.artifactory.api.search;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.log4j.Logger;
import org.artifactory.api.common.Info;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.maven.MavenArtifactInfo;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("searchResult")
public class SearchResult implements Info {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SearchResult.class);

    private final FileInfo fileInfo;
    private final MavenArtifactInfo artifact;
    private String lastModifiedString;

    public SearchResult(FileInfo fileInfo, MavenArtifactInfo artifact) {
        this.fileInfo = fileInfo;
        this.artifact = artifact;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getName() {
        return fileInfo.getName();
    }

    public String getRelDirPath() {
        return new File(fileInfo.getRelPath()).getParent();
    }

    public String getRepoKey() {
        return fileInfo.getRepoKey();
    }

    public String getPath() {
        return fileInfo.getRelPath();
    }

    public void setLastModifiedString(String lastModifiedString) {
        this.lastModifiedString = lastModifiedString;
    }

    public String getLastModifiedString() {
        return lastModifiedString;
    }

    public MavenArtifactInfo getArtifact() {
        return artifact;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchResult result = (SearchResult) o;
        return artifact.equals(result.artifact);
    }

    public int hashCode() {
        return artifact.hashCode();
    }
}
