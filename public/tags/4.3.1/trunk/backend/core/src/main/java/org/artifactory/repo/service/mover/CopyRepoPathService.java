package org.artifactory.repo.service.mover;


import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.sapi.fs.VfsItem;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
public class CopyRepoPathService extends RepoPathMover {

    @Override
    public void executeOperation(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        moveOrCopy(status, moverConfig);
    }

    @Override
    public void handleMoveOrCopy(MoveMultiStatusHolder status, MoverConfig moverConfig,
                                 VfsItem sourceItem, RepoRepoPath<LocalRepo> targetRrp) {
        DefaultRepoPathCopier defaultRepoPathCopier = new DefaultRepoPathCopier(status, moverConfig);
        if (moverConfig.isAtomic()) {
            defaultRepoPathCopier.moveOrCopy(sourceItem, targetRrp);
        } else {
            defaultRepoPathCopier.moveOrCopyMultiTx(sourceItem, targetRrp);
        }
    }

    @Override
    protected String operationType() {
        return "copy";
    }
}
