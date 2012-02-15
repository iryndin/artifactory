package org.artifactory.sapi.fs;

import org.artifactory.repo.RepoPath;

/**
 * Date: 8/4/11
 * Time: 3:27 PM
 *
 * @author Fred Simon
 */
public interface VfsMetadataAware {
    String METADATA_FOLDER = ".artifactory-metadata";

    /**
     * @return the absolute path of the this Metadata Aware item
     */
    String getAbsolutePath();

    /**
     * @return the repo path object of this item
     */
    RepoPath getRepoPath();

    boolean isFile();

    boolean isDirectory();
}
