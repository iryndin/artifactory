package org.artifactory.aql;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import org.artifactory.aql.model.AqlItemTypeEnum;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.result.rows.AqlItem;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.fs.MutableFolderInfo;
import org.artifactory.repo.RepoPath;

import java.util.Set;

/**
 * Converter from Aql entities to other data objects.
 *
 * @author Yossi Shaul
 */
public abstract class AqlConverts {
    public static final Function<AqlItem, ItemInfo> toFileInfo = new Function<AqlItem, ItemInfo>() {
        @Override
        public ItemInfo apply(AqlItem input) {
            RepoPath repoPath = AqlUtils.repoPathFromAql((AqlBaseFullRowImpl) input);
            AqlItemTypeEnum type = input.getType();
            if (AqlItemTypeEnum.folder == type) {
                MutableFolderInfo folderInfo = InfoFactoryHolder.get().createFolderInfo(repoPath);
                folderInfo.setCreated(input.getCreated().getTime());
                folderInfo.setLastUpdated(input.getUpdated().getTime());
                folderInfo.setCreatedBy(input.getCreatedBy());
                folderInfo.setModifiedBy(input.getModifiedBy());
                return folderInfo;
            } else {
                MutableFileInfo fileInfo = InfoFactoryHolder.get().createFileInfo(repoPath);
                fileInfo.setSize(input.getSize());
                fileInfo.setCreated(input.getCreated().getTime());
                fileInfo.setLastUpdated(input.getUpdated().getTime());
                fileInfo.setCreatedBy(input.getCreatedBy());
                fileInfo.setModifiedBy(input.getModifiedBy());
                Set<ChecksumInfo> checksums = Sets.newHashSet();
                checksums.add(new ChecksumInfo(ChecksumType.md5, input.getOriginalMd5(), input.getActualMd5()));
                checksums.add(new ChecksumInfo(ChecksumType.sha1, input.getOriginalSha1(), input.getActualSha1()));
                fileInfo.setChecksums(checksums);
                return fileInfo;
            }
        }
    };
}
