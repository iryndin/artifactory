package org.artifactory.repo.service.mover;

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chen Keinan
 */
public class DefaultRepoPathCopier extends BaseRepoPathMover {
    private static final Logger log = LoggerFactory.getLogger(DefaultRepoPathCopier.class);


    protected DefaultRepoPathCopier(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        super(status, moverConfig);
    }

    @Override
    protected void beforeOperationOnFolder(VfsItem sourceItem, RepoPath targetRepoPath) {
        storageInterceptors.beforeCopy(sourceItem, targetRepoPath, status, properties);
    }

    @Override
    public void beforeOperationOnFile(VfsItem sourceItem, RepoPath targetRepoPath) {
        storageInterceptors.beforeCopy(sourceItem, targetRepoPath, status, properties);
    }

    @Override
    protected void afterOperationOnFolder(VfsItem sourceItem, RepoRepoPath<LocalRepo> targetRrp,
                                          VfsFolder targetFolder) {
        afterFolderCopy(sourceItem, targetRrp, targetFolder, properties);
    }

    @Override
    public void operationOnFile(VfsFile sourceItem, RepoRepoPath<LocalRepo> targetRrp) {
        copyFile(sourceItem, targetRrp, properties);
    }

    @Override
    public VfsItem getSourceItem(VfsItem sourceItem) {
        return sourceItem;
    }
}
