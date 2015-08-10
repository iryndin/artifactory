package org.artifactory.ui.rest.model.builds;

import java.util.Collection;
import java.util.Set;

import org.artifactory.api.license.LicenseModuleModel;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen Keinan
 */
public class BuildLicenseModel extends BaseModel {

    private Collection<LicenseModuleModel> licenses;
    private Set<LicenseModuleModel> publishedModules;
    private Set<String> scopes;

    public BuildLicenseModel() {
    }

    public BuildLicenseModel(Collection<LicenseModuleModel> values,
                             Set<LicenseModuleModel> publishedModules, Set<String> scopes) {
        this.licenses = values;
        this.publishedModules = publishedModules;
        this.scopes = scopes;
    }

    public Collection<LicenseModuleModel> getLicenses() {
        return licenses;
    }

    public void setLicenses(Collection<LicenseModuleModel> licenses) {
        this.licenses = licenses;
    }

    public Set<LicenseModuleModel> getPublishedModules() {
        return publishedModules;
    }

    public void setPublishedModules(Set<LicenseModuleModel> publishedModules) {
        this.publishedModules = publishedModules;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }
}
