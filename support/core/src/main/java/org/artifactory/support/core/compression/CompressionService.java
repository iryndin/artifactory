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

package org.artifactory.support.core.compression;

import java.io.File;
import java.util.List;

/**
 * Provides content compression services
 *
 * @author Michael Pasternak
 */
public interface CompressionService {
    public static final String ARCHIVE_EXTENSION = "zip";
    /**
     * Compresses given directory into zip file
     *
     * @param directory the content to compress
     * @param size default archive size
     *
     * @return zipped archive/s
     */
    List<File> compress(File directory, int size);
}
