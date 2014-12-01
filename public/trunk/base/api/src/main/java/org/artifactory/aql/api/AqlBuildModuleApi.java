package org.artifactory.aql.api;

import org.artifactory.aql.result.rows.AqlBuildModule;

/**
 * @author Gidi Shabat
 */
public class AqlBuildModuleApi extends Aql<AqlBuildModuleApi, AqlBuildModule> {
    public AqlBuildModuleApi(Class<AqlBuildModule> domainClass) {
        super(domainClass);
    }
}