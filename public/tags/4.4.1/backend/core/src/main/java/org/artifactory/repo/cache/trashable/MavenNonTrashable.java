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

package org.artifactory.repo.cache.trashable;

import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.service.trash.NonTrashableItem;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
public class MavenNonTrashable implements NonTrashableItem {

    @Override
    public boolean skipTrash(LocalRepoDescriptor descriptor, String path) {
        boolean mavenSupport = RepoType.Maven.equals(descriptor.getType());
        return mavenSupport && (MavenNaming.isMavenMetadata(path) || MavenNaming.isIndex(path));
    }
}
