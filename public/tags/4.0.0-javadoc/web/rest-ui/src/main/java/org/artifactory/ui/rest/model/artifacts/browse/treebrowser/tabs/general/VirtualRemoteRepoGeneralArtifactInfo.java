package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general;

import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.info.BaseInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.info.RepositoryInfo;

/**
 * @author Chen Keinan
 */
public class VirtualRemoteRepoGeneralArtifactInfo extends BaseArtifactInfo {

    private BaseInfo info;
    private String offlineMessage;
    private String blackedOutMessage;

    public VirtualRemoteRepoGeneralArtifactInfo() {
    }

    public VirtualRemoteRepoGeneralArtifactInfo(String name) {
        super(name);
    }

    public void populateGeneralData(RepoBaseDescriptor repoBaseDescriptor) {
        RepoPath repoPath = InternalRepoPathFactory.create(repoBaseDescriptor.getKey(), "");
        BaseInfo baseInfo = populateVirtualRemoteRepositoryInfo(repoBaseDescriptor, repoPath);
        this.info = baseInfo;
    }

    /**
     * populate Repository info data
     *
     * @param repoDescriptor - repo descriptor
     * @param repoPath       - repo path
     * @return
     */
    private BaseInfo populateVirtualRemoteRepositoryInfo(RepoBaseDescriptor repoDescriptor, RepoPath repoPath) {
        RepositoryInfo repoInfo = new RepositoryInfo();
        if (repoDescriptor instanceof VirtualRepoDescriptor) {
            repoInfo.populateVirtualRepositoryInfo(repoDescriptor, repoPath);
        } else {
            repoInfo.populateRemoteRepositoryInfo(repoDescriptor, repoPath);
        }
        setRepositoryOffline(repoDescriptor);
        setRepositoryBlackedOut(repoDescriptor);
        return repoInfo;
    }

    private void setRepositoryOffline(RepoBaseDescriptor repoDescriptor) {
        if (repoDescriptor != null && repoDescriptor instanceof HttpRepoDescriptor) {
            if (((HttpRepoDescriptor) repoDescriptor).isOffline()) {
                this.setOfflineMessage("This repository is offline, content is served from the cache only.");
            }
        }
    }

    private void setRepositoryBlackedOut(RepoBaseDescriptor repoDescriptor) {
        if (repoDescriptor != null && repoDescriptor instanceof RealRepoDescriptor) {
            if (((RealRepoDescriptor) repoDescriptor).isBlackedOut()) {
                this.setBlackedOutMessage("This repository is blacked out, " +
                        "items can only be viewed but cannot be resolved or deployed.");
            }
        }
    }

    public BaseInfo getInfo() {
        return info;
    }

    public void setInfo(BaseInfo info) {
        this.info = info;
    }

    public String getOfflineMessage() {
        return offlineMessage;
    }

    public void setOfflineMessage(String offlineMessage) {
        this.offlineMessage = offlineMessage;
    }

    public String getBlackedOutMessage() {
        return blackedOutMessage;
    }

    public void setBlackedOutMessage(String blackedOutMessage) {
        this.blackedOutMessage = blackedOutMessage;
    }

    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
