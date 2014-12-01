package org.artifactory.aql.api;

import org.artifactory.aql.result.rows.AqlStatistics;

/**
 * @author Gidi Shabat
 */
public class AqlStatisticsApi extends Aql<AqlStatisticsApi, AqlStatistics> {
    public AqlStatisticsApi(Class<AqlStatistics> domainClass) {
        super(domainClass);
    }
}