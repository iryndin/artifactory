package org.artifactory.ui.rest.model.artifacts.search.trashsearch;

import org.artifactory.ui.rest.model.artifacts.search.BaseSearch;


/**
 * @author Shay Yaakov
 */
public class TrashSearch extends BaseSearch {

    private String query;
    private Boolean isChecksum;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Boolean getIsChecksum() {
        return isChecksum;
    }

    public void setIsChecksum(Boolean isChecksum) {
        this.isChecksum = isChecksum;
    }
}
