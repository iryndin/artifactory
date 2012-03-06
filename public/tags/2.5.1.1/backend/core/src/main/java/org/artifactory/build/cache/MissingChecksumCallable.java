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

package org.artifactory.build.cache;

import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.repo.RepoPath;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A customized callable that searches for missing checksums
 *
 * @author Noam Y. Tenne
 */
public class MissingChecksumCallable implements Callable<ChecksumPair> {

    private String sha1;
    private String md5;

    /**
     * Default constructor
     *
     * @param sha1 SHA1 checksum value
     * @param md5  MD5 checksum value
     */
    public MissingChecksumCallable(String sha1, String md5) {
        this.sha1 = sha1;
        this.md5 = md5;
    }

    @Override
    public ChecksumPair call() throws Exception {
        InternalArtifactoryContext context = InternalContextHelper.get();
        SearchService searchService = context.beanForType(SearchService.class);
        ChecksumSearchControls controls = new ChecksumSearchControls();
        controls.addChecksum(ChecksumType.md5, md5);
        controls.addChecksum(ChecksumType.sha1, sha1);
        Set<RepoPath> artifactList = searchService.searchArtifactsByChecksum(controls);
        if (!artifactList.isEmpty()) {
            RepoPath repoPath = artifactList.iterator().next();
            MutableFileInfo fileInfo = new FileInfoProxy(repoPath);
            return new FoundChecksumPair(fileInfo.getSha1(), fileInfo.getMd5());
        }
        return new UnfoundChecksumPair();
    }
}
