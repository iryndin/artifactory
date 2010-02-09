package org.artifactory.api.repo;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * Provides artifacts deploy services.
 *
 * @author Yossi Shaul
 */
public interface DeployService {

    @Request
    @Lock(transactional = true)
    void deploy(RepoDescriptor targetRepo, MavenArtifactInfo artifactInfo, File uploadedFile, boolean forceDeployPom,
                boolean partOfBundleDeploy) throws RepoAccessException;

    @Request
    @Lock(transactional = true)
    void deploy(RepoDescriptor targetRepo, MavenArtifactInfo artifactInfo,
                File file, String pomString, boolean forceDeployPom, boolean partOfBundleDeploy) throws RepoAccessException;

    @Request
    void deployBundle(File bundle, RealRepoDescriptor targetRepo, StatusHolder status);

    /**
     * Validates pom before deployment.
     *
     * @param pomContent                   Pom content to validate
     * @param relPath                      Relative deployment path of the pom
     * @param suppressPomConsistencyChecks If true will not throw an exception is pom consistency fails (eg, groupId
     *                                     doesn't match target relative path)
     * @throws IOException If pom is invalid
     */
    public void validatePom(String pomContent, String relPath, boolean suppressPomConsistencyChecks)
            throws IOException;


    /**
     * Get the artifact model from a jar or pom file
     *
     * @param uploadedFile .jar or .pom file
     * @return null if no pom found
     * @throws java.io.IOException if uploaded file is unreadable
     */
    MavenArtifactInfo getArtifactInfo(File uploadedFile);

    public String getModelString(MavenArtifactInfo artifactInfo);

}
