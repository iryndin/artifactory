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

package org.artifactory.support.utils;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Pasternak
 */
public class FileUtils {

    /**
     * Converts list of {@link java.io.File} to
     * list of {@link java.lang.String} file names
     *
     * @param files
     *
     * @return List<String>
     */
    public static List<String> toFileNames(List<File> files) {
        return files != null && files.size() > 0 ?
                files.parallelStream()
                        .map(f -> f.getName())
                        .collect(Collectors.toList())
                :
                Lists.newLinkedList();
    }
}
