package org.artifactory.api.bintray.docker;

import org.artifactory.repo.RepoPath;

import java.util.Map;

/**
 * @author Dan Feldman
 */
public class DockerV2BintrayInfo {

    public RepoPath manifestFile;
    public Map<String, RepoPath> imageDigestToPath;
    public RepoPath tagPath;

    public DockerV2BintrayInfo(RepoPath manifestFile, Map<String, RepoPath> imageDigestToPath, RepoPath tagPath) {
        this.manifestFile = manifestFile;
        this.imageDigestToPath = imageDigestToPath;
        this.tagPath = tagPath;
    }
}
