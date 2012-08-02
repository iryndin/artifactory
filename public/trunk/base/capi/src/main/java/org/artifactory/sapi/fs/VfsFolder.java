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

package org.artifactory.sapi.fs;

import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.MutableFolderInfo;
import org.artifactory.sapi.common.ExportSettings;

import java.util.List;

/**
 * Date: 8/4/11
 * Time: 1:40 PM
 *
 * @author Fred Simon
 */
public interface VfsFolder extends VfsItem<FolderInfo, MutableFolderInfo> {
    List<VfsItem> getItems(boolean withWriteLock);

    void exportTo(ExportSettings settings);
}
