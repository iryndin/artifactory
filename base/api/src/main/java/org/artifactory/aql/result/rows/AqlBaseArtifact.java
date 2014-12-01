package org.artifactory.aql.result.rows;

import static org.artifactory.aql.model.AqlDomainEnum.artifacts;


/**
 * @author Gidi Shabat
 */
@QueryTypes({artifacts})
public interface AqlBaseArtifact extends AqlRowResult {
    int getType();

    String getRepo();

    String getPath();

    String getName();
}
