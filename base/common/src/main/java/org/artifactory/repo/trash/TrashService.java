package org.artifactory.repo.trash;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.common.StatusHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.spring.ReloadableBean;

/**
 * @author Shay Yaakov
 */
public interface TrashService extends ReloadableBean {

    String TRASH_KEY = "auto-trashcan";
    String PROP_TRASH_TIME = "trash.time";
    String PROP_DELETED_BY = "trash.deletedBy";
    String PROP_ORIGIN_REPO = "trash.originalRepository";
    String PROP_ORIGIN_REPO_TYPE = "trash.originalRepositoryType";
    String PROP_ORIGIN_PATH = "trash.originalPath";
    String PROP_RESTORED_TIME = "trash.restoredTime";

    /**
     * Copies the given repoPath to the trashcan if it's a file, overriding properties if it's a folder.
     * Usually this will be called after all beforeDelete events and before any afterDelete events.
     *
     * @param repoPath The repo path to trash
     */
    void copyToTrash(RepoPath repoPath);

    /**
     * Restores an item from the trashcan to it's original repository path.
     *
     * @param repoPath The repo path to restore
     * @param restoreRepo The restore repo key
     * @param restorePath The restore repo path (can be a file for renaming or a folder)
     */
    BasicStatusHolder restore(RepoPath repoPath, String restoreRepo, String restorePath);

    /**
     * Removes all the item from the trashcan
     */
    StatusHolder empty();
}