package org.artifactory.aql.result.rows;

import java.util.Date;

import static org.artifactory.aql.model.AqlDomainEnum.artifacts;
import static org.artifactory.aql.model.AqlDomainEnum.build_dependencies;

/**
 * @author Gidi Shabat
 */
@QueryTypes({artifacts, build_dependencies})
public interface AqlArtifactsWithBuildDependencies extends AqlRowResult {
    Date getCreated();

    Date getModified();

    Date getUpdated();

    String getCreatedBy();

    Date getModifiedBy();

    int getType();

    String getRepo();

    String getPath();

    String getName();

    int getDepth();

    int getNodeId();

    String getOriginalMd5();

    String getActualMd5();

    String getOriginalSha1();

    String getActualSha1();

    String getBuildDependencyName();

    String getBuildDependencyScope();

    String getBuildDependencyType();

    String getBuildDependencySha1();

    String getBuildDependencyMd5();
}
