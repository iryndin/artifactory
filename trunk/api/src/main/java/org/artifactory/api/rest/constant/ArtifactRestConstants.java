/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.api.rest.constant;

/**
 * @author Eli Givoni
 */
public interface ArtifactRestConstants {
    String PATH_ROOT = "storage";
    String MT_FOLDER_INFO = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".FolderInfo+json";
    String MT_FILE_INFO = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".FileInfo+json";
    String MT_ITEM_METADATA_NAMES = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".ItemMetadataNames+json";
    String MT_ITEM_METADATA = RestConstants.MT_ARTIFACTORY_APP + PATH_ROOT + ".ItemMetadata+json";

    String METADATA_NAMES_PREFIX = "mdns";

}
