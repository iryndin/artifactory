package org.artifactory.api.search;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;

import java.util.List;

/**
 * Holds a list of search results
 *
 * @author Noam Tenne
 */
@XStreamAlias("searchResultGroup")
public class SearchResultGroup implements Info {

    private List<SearchResult> resultGroup;

    /**
     * Constructor
     *
     * @param resultGroup - A list of SearchResult objects
     */
    public SearchResultGroup(List<SearchResult> resultGroup) {
        this.resultGroup = resultGroup;
    }

    /**
     * Returns the result group
     *
     * @return List<SearchResult> - A list of SearchResult objects
     */
    public List<SearchResult> getResultGroup() {
        return resultGroup;
    }

    /**
     * Sets the result group
     *
     * @param resultGroup - A list of SearchResult objects
     */
    public void setResultGroup(List<SearchResult> resultGroup) {
        this.resultGroup = resultGroup;
    }
}