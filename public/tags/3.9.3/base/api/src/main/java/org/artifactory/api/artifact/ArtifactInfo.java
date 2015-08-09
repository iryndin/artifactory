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

package org.artifactory.api.artifact;

import org.apache.commons.lang.StringUtils;

/**
 * Generic Artifact Info
 *
 * @author Tomer Cohen
 */
public class ArtifactInfo implements UnitInfo {

    private final String path;

    /**
     * Artifact info constructor, which takes a path.
     *
     * @param path The path of deployment for the artifact.
     */
    public ArtifactInfo(String path) {
        this.path = path;
    }

    /**
     * {@inheritDoc}
     *
     * @return False, since this is a generic artifact.
     */
    @Override
    public boolean isMavenArtifact() {
        return false;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isValid() {
        return StringUtils.isNotBlank(path) && !NA.equals(path);
    }
}
