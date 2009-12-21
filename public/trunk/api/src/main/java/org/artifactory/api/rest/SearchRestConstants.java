/*
 * This file is part of Artifactory.
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

package org.artifactory.api.rest;

/**
 * @author yoavl
 */
public interface SearchRestConstants {
    String SEARCH_PATH_ROOT = "search";
    String SEARCH_PATH_ARTIFACT = "artifact";
    String SEARCH_PATH_METADATA = "metadata";
    String SEARCH_PATH_ARCHIVE = "archive";

    String SEARCH_PARAM_ARTIFACT_QUERY = "query";

    String SEARCH_PARAM_ARCHIVE_QUERY = "query";
    String SEARCH_PARAM_ARCHIVE_SEARCH_ALL_TYPES = "searchAllTypes";

    String SEARCH_PARAM_METADATA_METADATA_NAME = "metadataName";
    String SEARCH_PARAM_METADATA_PATH = "path";
    String SEARCH_PARAM_METADATA_VALUE = "value";
    String SEARCH_PARAM_METADATA_EXACT_MATCH = "exactMatch";
}