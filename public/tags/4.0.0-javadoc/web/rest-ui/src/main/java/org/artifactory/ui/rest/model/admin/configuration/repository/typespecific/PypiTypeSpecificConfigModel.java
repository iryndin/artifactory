package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.configuration.repository.RepoConfigDefaultValues;

/**
 * @author Dan Feldman
 */
public class PypiTypeSpecificConfigModel implements TypeSpecificConfigModel {

    @Override
    public RepoType getRepoType() {
        return RepoType.Pypi;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.PYPI_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
