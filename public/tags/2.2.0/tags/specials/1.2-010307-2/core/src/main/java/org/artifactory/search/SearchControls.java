package org.artifactory.search;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * @author Ben Walding
 */
public class SearchControls implements Serializable {
    private String search;

    public SearchControls() {
    }

    public SearchControls(HttpServletRequest request) {
        this.search = request.getParameter("search");
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }


    public String toString() {
        return "SearchControls{" +
                "search='" + search + '\'' +
                '}';
    }
}