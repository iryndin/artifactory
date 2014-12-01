package org.artifactory.aql.result.rows;

import java.util.Date;

import static org.artifactory.aql.model.AqlDomainEnum.full_artifacts;

/**
 * @author Gidi Shabat
 */
@QueryTypes(full_artifacts)
public interface AqlArtifact extends AqlRowResult {
    Date getCreated();

    Date getModified();

    Date getUpdated();

    String getCreatedBy();

    String getModifiedBy();

    int getType();

    String getRepo();

    String getPath();

    String getName();

    long getSize();

    int getDepth();

    int getNodeId();

    String getOriginalMd5();

    String getActualMd5();

    String getOriginalSha1();

    String getActualSha1();
}
