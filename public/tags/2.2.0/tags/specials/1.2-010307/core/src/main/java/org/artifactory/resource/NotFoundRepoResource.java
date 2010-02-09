package org.artifactory.resource;

import org.artifactory.repo.Repo;

import java.util.Date;

public class NotFoundRepoResource extends SimpleRepoResource {

    /**
     * Use to store a 'snapshot not found' in the snapshot cache
     *
     * @param relPath
     * @param repo
     */
    public NotFoundRepoResource(String relPath, Repo repo) {
        super(relPath, repo);
        setLastModifiedTime(new Date().getTime());
    }

    public boolean isFound() {
        return false;
    }
}