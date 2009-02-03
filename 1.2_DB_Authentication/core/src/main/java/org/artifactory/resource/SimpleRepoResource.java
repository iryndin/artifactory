package org.artifactory.resource;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrFile;
import org.artifactory.repo.Repo;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleRepoResource implements RepoResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleRepoResource.class);

    /**
     * relative path of the file, excluding a leading / and the file name.
     */
    private String relPath;
    private String repoKey;
    private String name;
    private Date lastModified;
    private Date lastUpdated;
    private long size;

    public SimpleRepoResource(JcrFile jcrFile) {
        setRepoKey(jcrFile.repoKey());
        setRelPath(jcrFile.relPath());
        setLastModified(jcrFile.lastModified());
        setSize(jcrFile.size());
        setLastUpdated(jcrFile.lastUpdated());
    }

    protected SimpleRepoResource() {
    }

    public SimpleRepoResource(String relPath, Repo repo) {
        setRelPath(relPath);
        repoKey = repo.getKey();
    }

    public String getRelPath() {
        return relPath;
    }

    public String getRelDirPath() {
        return relPath.substring(0, relPath.lastIndexOf("/"));
    }

    public String getAbsPath() {
        return "/" + repoKey + "/" + relPath;
    }

    public String getName() {
        return name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getLastModifiedTime() {
        return lastModified.getTime();
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public long getAge() {
        return lastUpdated != null ? System.currentTimeMillis() - lastUpdated.getTime() : -1;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public long getSize() {
        return size;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModifiedTime(long lastModified) {
        this.lastModified = new Date(lastModified);
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isFound() {
        return true;
    }

    protected void setRelPath(String relPath) {
        this.relPath = relPath;
        name = relPath.substring(relPath.lastIndexOf('/') + 1);
    }

    protected void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleRepoResource resource = (SimpleRepoResource) o;
        return relPath.equals(resource.relPath) && repoKey.equals(resource.repoKey);

    }

    public int hashCode() {
        int result;
        result = relPath.hashCode();
        result = 31 * result + repoKey.hashCode();
        return result;
    }

    public String toString() {
        return "{" + repoKey + ", " + relPath + "}";
    }
}
