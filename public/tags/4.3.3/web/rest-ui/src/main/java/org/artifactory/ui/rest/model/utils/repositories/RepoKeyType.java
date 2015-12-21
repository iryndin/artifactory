package org.artifactory.ui.rest.model.utils.repositories;

import org.artifactory.descriptor.repo.DockerApiVersion;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen keinan
 */
public class RepoKeyType extends BaseModel {

    private String repoKey;
    private RepoType RepoType;
    private String dockerApiVersion;
    private String type;
    private Boolean canDeploy;
    private Boolean canRead;
    private Boolean isLocal;
    private Boolean isRemote;
    private Boolean isVirtual;

    public RepoKeyType(){}

    public RepoKeyType(String type, String repoKey) {
        this.repoKey = repoKey;
        this.type = type;
    }

    public RepoKeyType(RepoType repoType, String repoKey) {
        this.repoKey = repoKey;
        this.RepoType = repoType;
    }

    public RepoKeyType(String type, RepoType repoType, String repoKey) {
        this.type = type;
        this.repoKey = repoKey;
        this.RepoType = repoType;
    }

    public RepoKeyType(String type, RepoType repoType, String repoKey, DockerApiVersion dockerApiVersion) {
        this.repoKey = repoKey;
        this.RepoType = repoType;
        this.type = type;
        this.dockerApiVersion = dockerApiVersion.name();
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public org.artifactory.descriptor.repo.RepoType getRepoType() {
        return RepoType;
    }

    public void setRepoType(org.artifactory.descriptor.repo.RepoType repoType) {
        RepoType = repoType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getCanDeploy() {
        return canDeploy;
    }

    public void setCanDeploy(Boolean canDeploy) {
        this.canDeploy = canDeploy;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public Boolean getIsLocal() {
        return isLocal;
    }

    public void setIsLocal(Boolean isLocal) {
        this.isLocal = isLocal;
    }

    public Boolean getIsRemote() {
        return isRemote;
    }

    public void setIsRemote(Boolean isRemote) {
        this.isRemote = isRemote;
    }

    public Boolean getIsVirtual() {
        return isVirtual;
    }

    public void setIsVirtual(Boolean isVirtual) {
        this.isVirtual = isVirtual;
    }

    public String getDockerApiVersion() {
        return dockerApiVersion;
    }
}
