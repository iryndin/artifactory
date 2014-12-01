package org.artifactory.aql.api;

import org.artifactory.aql.result.rows.AqlArchiveArtifact;

/**
 * @author Gidi Shabat
 */
public class AqlArchiveApi extends Aql<AqlArchiveApi, AqlArchiveArtifact> {
    public AqlArchiveApi(Class<AqlArchiveArtifact> domainClass) {
        super(domainClass);
    }
}
