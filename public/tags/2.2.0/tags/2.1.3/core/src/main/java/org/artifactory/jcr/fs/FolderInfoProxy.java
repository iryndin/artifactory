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

package org.artifactory.jcr.fs;

import org.artifactory.api.fs.FolderAdditionalInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Yoav Landman
 */
public class FolderInfoProxy extends ItemInfoProxy<FolderInfo> implements FolderInfo {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(FolderInfoProxy.class);

    public FolderInfoProxy(RepoPath repoPath) {
        super(repoPath);
    }

    public boolean isFolder() {
        //Do not materialize
        return true;
    }

    public FolderAdditionalInfo getInternalXmlInfo() {
        return getMaterialized().getInternalXmlInfo();
    }

    public void setAdditionalInfo(FolderAdditionalInfo additionalInfo) {
        getMaterialized().setAdditionalInfo(additionalInfo);
    }
}