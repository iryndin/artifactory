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

package org.artifactory.jcr.version.v150;

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * To be called when changes are being made in the indexing mechanisms.<br> All the old index is simply being removed,
 * since these changes usually require a complete rebuild
 *
 * @author Noam Y. Tenne
 */
public class IndexRemover implements ConfigurationConverter<ArtifactoryHome> {

    private static final Logger log = LoggerFactory.getLogger(IndexRemover.class);

    @Override
    public void convert(ArtifactoryHome artifactoryHome) {
        File indexDir = new File(artifactoryHome.getDataDir(), "index");
        try {
            FileUtils.deleteDirectory(indexDir);
        } catch (IOException e) {
            log.warn("Failed to remove index at {}.", indexDir.getAbsolutePath());
        }
    }
}
