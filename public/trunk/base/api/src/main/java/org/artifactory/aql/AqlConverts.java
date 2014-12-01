package org.artifactory.aql;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import org.artifactory.aql.result.rows.AqlArtifact;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;

import java.util.Set;

/**
 * Converter from Aql entities to other data objects.
 *
 * @author Yossi Shaul
 */
public abstract class AqlConverts {
    public static final Function<AqlArtifact, FileInfo> toFileInfo = new Function<AqlArtifact, FileInfo>() {
        @Override
        public FileInfo apply(AqlArtifact input) {
            RepoPath repoPath = RepoPathFactory.create(input.getRepo(), input.getPath() + "/" + input.getName());
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
    };
}
