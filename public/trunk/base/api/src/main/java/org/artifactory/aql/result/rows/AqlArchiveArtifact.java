package org.artifactory.aql.result.rows;

import static org.artifactory.aql.model.AqlDomainEnum.archive;
import static org.artifactory.aql.model.AqlDomainEnum.full_artifacts;

/**
 * @author Gidi Shabat
 */
@QueryTypes({archive, full_artifacts})
public interface AqlArchiveArtifact extends AqlArtifact {
    String getEntryName();

    String getEntryPath();
}
