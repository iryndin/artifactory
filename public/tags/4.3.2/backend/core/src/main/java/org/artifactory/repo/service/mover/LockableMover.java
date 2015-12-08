package org.artifactory.repo.service.mover;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.md.Properties;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.sapi.common.Lock;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;

/**
 * @author Chen Keinan
 */
public interface LockableMover {

    @Lock
    void moveCopyFile(VfsItem source, RepoRepoPath<LocalRepo> targetRrp, BaseRepoPathMover repoPathMover, MoveMultiStatusHolder status);

    @Lock
    VfsFolderRepo moveCopyFolder(VfsItem source, RepoRepoPath<LocalRepo> targetRrp, BaseRepoPathMover repoPathMove, MoveMultiStatusHolder status);

    @Lock
    void postFolderProcessing(VfsItem sourceItem, VfsFolder targetFolder, RepoRepoPath<LocalRepo> targetRrp, BaseRepoPathMover repoPathMover,
                              Properties properties, int numOfChildren);
}
