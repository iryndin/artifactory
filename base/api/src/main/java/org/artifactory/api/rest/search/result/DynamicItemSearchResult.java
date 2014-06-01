/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: 5/11/14 2:51 PM
 *
 * @author freds
 */
public class DynamicItemSearchResult {
    public List<SearchEntry> results = new ArrayList<>();

    public static class SearchEntry {
        public boolean folder;
        public String repo;
        public String path;
        public String created;
        public String createdBy;
        public String lastModified;
        public String modifiedBy;
        public String lastUpdated;
        public String downloadCount;
        public String lastDownloaded;
        public Map<String, String[]> properties;
        public String mimeType;
        public String size;
        public Checksums checksums;
        public Checksums originalChecksums;

        public SearchEntry() {
        }

        public static class Checksums {
            public String sha1;
            public String md5;

            public Checksums(String sha1, String md5) {
                this.sha1 = sha1;
                this.md5 = md5;
            }

            private Checksums() {
            }
        }
    }
}
