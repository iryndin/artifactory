package org.artifactory.aql.api;

import org.artifactory.aql.result.rows.AqlBuildProperty;

/**
 * @author Gidi Shabat
 */
public class AqlBuildPropertyApi extends Aql<AqlBuildPropertyApi, AqlBuildProperty> {
    public AqlBuildPropertyApi(Class<AqlBuildProperty> domainClass) {
        super(domainClass);
    }
}