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

package org.artifactory.repo.index.locator;

import org.sonatype.nexus.index.locator.Locator;

import java.io.File;

/**
 * @author yoavl
 */
public class MetadataLocator implements Locator {

    public File locate(File source) {
        //We never locate metadata
        //return new File(source.getParentFile().getParentFile() - may throw NPE, "maven-metadata.xml") {
        return new File("", "maven-metadata.xml") {
            @Override
            public boolean exists() {
                return false;
            }
        };
    }
}