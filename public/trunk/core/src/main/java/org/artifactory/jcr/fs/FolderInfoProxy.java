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

package org.artifactory.jcr.fs;

import org.artifactory.api.fs.FolderAdditionalInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.repo.RepoPath;

/**
 * @author Yoav Landman
 */
public class FolderInfoProxy extends ItemInfoProxy<FolderInfo> implements FolderInfo {

    public FolderInfoProxy(RepoPath repoPath) {
        super(repoPath);
    }

    public boolean isFolder() {
        //Do not materialize
        return true;
    }

    public FolderAdditionalInfo getAdditionalInfo() {
        return getMaterialized().getAdditionalInfo();
    }

    public void setAdditionalInfo(FolderAdditionalInfo additionalInfo) {
        getMaterialized().setAdditionalInfo(additionalInfo);
    }
}