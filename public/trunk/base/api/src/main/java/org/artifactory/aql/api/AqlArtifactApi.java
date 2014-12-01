package org.artifactory.aql.api;

import org.artifactory.aql.result.rows.AqlArtifact;

/**
 * @author Gidi Shabat
 */
public class AqlArtifactApi extends Aql<AqlArtifactApi, AqlArtifact> {
    public AqlArtifactApi(Class<AqlArtifact> domainClass) {
        super(domainClass);
    }
}
