/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.api.rest.search.result;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Search result object to be returned by REST checksum searches
 *
 * @author Noam Y. Tenne
 */
public class ChecksumRestSearchResult {

    private Set<SearchEntry> results = Sets.newHashSet();

    public ChecksumRestSearchResult(Set<SearchEntry> results) {
        this.results = results;
    }

    public ChecksumRestSearchResult() {
    }

    public Set<SearchEntry> getResults() {
        return results;
    }

    public void setResults(Set<SearchEntry> results) {
        this.results = results;
    }

    public void addResult(SearchEntry searchEntry) {
        if (results == null) {
            results = Sets.newHashSet();
        }
        results.add(searchEntry);
    }

    public static class SearchEntry {
        private String uri;

        public SearchEntry(String uri) {
            this.uri = uri;
        }

        private SearchEntry() {
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }
}
