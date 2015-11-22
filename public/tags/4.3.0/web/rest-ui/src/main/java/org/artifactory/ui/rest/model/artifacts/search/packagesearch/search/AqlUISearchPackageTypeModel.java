package org.artifactory.ui.rest.model.artifacts.search.packagesearch.search;

import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AqlUISearchPackageTypeModel extends BaseModel {

    //both
    private String id;
    private String displayName;
    private String icon;

    public AqlUISearchPackageTypeModel(PackageSearchCriteria.PackageSearchType type) {
        this.id = type.getId();
        this.displayName = type.getDisplayName();
        this.icon = type.getIcon();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}