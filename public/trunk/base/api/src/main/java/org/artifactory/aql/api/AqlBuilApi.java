package org.artifactory.aql.api;

import org.artifactory.aql.result.rows.AqlBuild;

/**
 * @author Gidi Shabat
 */
public class AqlBuilApi extends Aql<AqlBuilApi, AqlBuild> {
    public AqlBuilApi(Class<AqlBuild> domainClass) {
        super(domainClass);
    }
}