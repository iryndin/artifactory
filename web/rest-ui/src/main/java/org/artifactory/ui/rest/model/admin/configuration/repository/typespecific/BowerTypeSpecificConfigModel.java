package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues;

import java.util.List;

import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_BOWER_REGISTRY;

/**
 * @author Dan Feldman
 */
public class BowerTypeSpecificConfigModel extends VcsTypeSpecificConfigModel {

    //remote
    private String registryUrl = DEFAULT_BOWER_REGISTRY;

    //virtual
    private Boolean enableExternalDependencies = false;
    private List<String> externalPatterns = Lists.newArrayList("**");
    private String externalRemoteRepo = "";

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public Boolean getEnableExternalDependencies() {
        return enableExternalDependencies;
    }

    public void setEnableExternalDependencies(Boolean enableExternalDependencies) {
        this.enableExternalDependencies = enableExternalDependencies;
    }

    public List<String> getExternalPatterns() {
        return externalPatterns;
    }

    public void setExternalPatterns(List<String> externalPatterns) {
        this.externalPatterns = externalPatterns;
    }

    public String getExternalRemoteRepo() {
        return externalRemoteRepo;
    }

    public void setExternalRemoteRepo(String externalRemoteRepo) {
        this.externalRemoteRepo = externalRemoteRepo;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.Bower;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.VCS_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
