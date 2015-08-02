package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.DockerApiVersion;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues;

import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_DOCKER_API_VER;
import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_TOKEN_AUTH;

/**
 * @author Dan Feldman
 */
public class DockerTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //local
    protected DockerApiVersion dockerApiVersion = DEFAULT_DOCKER_API_VER;

    //remote
    protected Boolean enableTokenAuthentication = DEFAULT_TOKEN_AUTH;


    public DockerApiVersion getDockerApiVersion() {
        return dockerApiVersion;
    }

    public void setDockerApiVersion(DockerApiVersion dockerApiVersion) {
        this.dockerApiVersion = dockerApiVersion;
    }

    public Boolean getEnableTokenAuthentication() {
        return enableTokenAuthentication;
    }

    public void setEnableTokenAuthentication(Boolean enableTokenAuthentication) {
        this.enableTokenAuthentication = enableTokenAuthentication;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Docker;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.DOCKER_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
