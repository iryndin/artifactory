package org.artifactory.repo.service.mover;

import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.sapi.fs.MutableVfsFolder;
import org.artifactory.sapi.fs.VfsItem;

/**
 * @author Chen Keinan
 */
public class VfsFolderRepo {

    private RepoRepoPath repoRepoPath;
    private VfsItem vfsItem;

    public VfsFolderRepo(MutableVfsFolder mutableVfsFolder, RepoRepoPath<LocalRepo> targetRrp) {
        this.repoRepoPath = targetRrp;
        this.vfsItem = mutableVfsFolder;
    }

    public RepoRepoPath getRepoRepoPath() {
        return repoRepoPath;
    }

    public void setRepoRepoPath(RepoRepoPath repoRepoPath) {
        this.repoRepoPath = repoRepoPath;
    }

    public VfsItem getVfsItem() {
        return vfsItem;
    }

    public void setVfsItem(VfsItem vfsItem) {
        this.vfsItem = vfsItem;
    }
}
