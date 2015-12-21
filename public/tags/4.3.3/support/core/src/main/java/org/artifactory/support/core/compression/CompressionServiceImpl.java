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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.artifactory.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides content compression services
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class CompressionServiceImpl implements CompressionService {

    private static final Logger log = LoggerFactory.getLogger(CompressionServiceImpl.class);

    /**
     * Compresses given directory into zip file
     *
     * @param directory the content to compress
     * @param size default archive size
     *
     * @return zipped archive/s
     */
    @Override
    public List<File> compress(File directory, int size) {

        List<File> destinationArchives = Lists.newLinkedList();
        try {
            File destinationArchive = new File(directory.getPath() + File.separator
                    + directory.getName() + "."+ ARCHIVE_EXTENSION);
            ZipUtils.archive(directory, destinationArchive, true);
            destinationArchives.add(destinationArchive);
        } catch (IOException e) {
            log.error("Content compression has failed, - " + e.getMessage());
            log.debug("Cause: {}", e);
        } finally {
            cleanup(directory);
        }
        return destinationArchives;
    }

    /**
     * Performs cleanup
     *
     * @param directory content to clean
     */
    private void cleanup(File directory) {
        try {
            Files.walk(directory.toPath(), FileVisitOption.FOLLOW_LINKS)
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(directory.toPath()))
                    .filter (p -> !FilenameUtils.getExtension(p.toString()).equals("zip"))
                    .forEach(p -> {
                        try {
                            FileUtils.deleteDirectory(p.toFile());
                        } catch (IOException e) {
                            log.debug("Cannot delete folder: {}", e);
                        }
                    } );
        } catch (IOException e) {
            log.debug("Cleanup has failed: {}", e);
        }
    }
}
