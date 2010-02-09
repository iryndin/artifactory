package org.artifactory.search;

import org.apache.log4j.Logger;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.security.SecuredResource;
import org.artifactory.utils.DateUtils;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SearchResult implements SecuredResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SearchResult.class);

    private ArtifactResource artifact;
    private String lastModifiedString;

    public SearchResult(ArtifactResource artifact) {
        this.artifact = artifact;
        lastModifiedString = DateUtils.format(artifact.getLastModified());
    }

    public String getName() {
        return artifact.getName();
    }

    public String getRelDirPath() {
        return artifact.getDirPath();
    }

    public String getRepoKey() {
        return artifact.getRepoKey();
    }

    public String getPath() {
        return artifact.getPath();
    }

    public String getLastModifiedString() {
        return lastModifiedString;
    }

    public ArtifactResource getArtifact() {
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
