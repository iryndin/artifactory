package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues;

/**
 * @author Dan Feldman
 */
public class GemsTypeSpecificConfigModel implements TypeSpecificConfigModel {

    @Override
    public RepoType getRepoType() {
        return RepoType.Gems;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.RUBYGEMS_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
