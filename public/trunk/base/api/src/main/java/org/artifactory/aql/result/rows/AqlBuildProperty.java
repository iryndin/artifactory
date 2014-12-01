package org.artifactory.aql.result.rows;

import static org.artifactory.aql.model.AqlDomainEnum.build_props;

/**
 * @author Gidi Shabat
 */
@QueryTypes({build_props})
public interface AqlBuildProperty extends AqlRowResult {
    String getBuildPropKey();

    String getBuildPropValue();
}
