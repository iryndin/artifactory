package org.artifactory.webapp.actionable;

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;

/**
 * @author Eli Givoni
 */
public interface FileActionable {
    FileInfo getFileInfo();

    RepoPath getRepoPath();
}
