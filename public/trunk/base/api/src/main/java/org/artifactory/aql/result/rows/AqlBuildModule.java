package org.artifactory.aql.result.rows;

import static org.artifactory.aql.model.AqlDomainEnum.build_module;

/**
 * @author Gidi Shabat
 */
@QueryTypes({build_module})
public interface AqlBuildModule extends AqlRowResult {
    String getBuildModuleName();
}
