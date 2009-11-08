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

package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.repo.RepoPath;

/**
 * @author yoavl
 */
@XStreamAlias(FolderInfo.ROOT)
public class FolderInfoImpl extends ItemInfoImpl implements FolderInfo {

    private FolderAdditionalInfo additionalInfo;

    public FolderInfoImpl(RepoPath repoPath) {
        super(repoPath);
        additionalInfo = new FolderAdditionalInfo();
    }

    public FolderInfoImpl(FolderInfo info) {
        super(info);
        additionalInfo = new FolderAdditionalInfo(info.getInternalXmlInfo());
    }

    /**
     * Required by xstream
     *
     * @param info
     */
    protected FolderInfoImpl(FolderInfoImpl info) {
        this(((FolderInfo) info));
    }

    /**
     * Should not be called by clients - for internal use
     *
     * @return
     */
    public void setAdditionalInfo(FolderAdditionalInfo additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public boolean isFolder() {
        return true;
    }

    @Override
    public String toString() {
        return "FolderInfo{" + super.toString() + ", extension=" + additionalInfo + '}';
    }

    @Override
    public boolean isIdentical(ItemInfo info) {
        return super.isIdentical(info);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Deprecated
    public FolderAdditionalInfo getInternalXmlInfo() {
        return additionalInfo;
    }
}