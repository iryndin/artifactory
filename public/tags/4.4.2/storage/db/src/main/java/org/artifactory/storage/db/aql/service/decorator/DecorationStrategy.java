package org.artifactory.storage.db.aql.service.decorator;

import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;

/**
 * @author Shay Yaakov
 */
public interface DecorationStrategy {

    void decorate(AqlQuery aqlQuery);
}
