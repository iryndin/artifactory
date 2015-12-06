package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues;

import java.util.List;

import static org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues.DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

/**
 * @author Dan Feldman
 */
public class NpmTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //virtual
    private Boolean enableExternalDependencies = false;
    private List<String> externalPatterns = Lists.newArrayList("**");
    private String externalRemoteRepo = "";

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
        return RepoType.Npm;
    }

    protected Boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

    public Boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(Boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.NPM_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
