package org.artifactory.storage.db.aql.service;

import org.artifactory.storage.db.aql.service.decorator.DecorationStrategy;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;

/**
 * @author Gidi Shabat
 */
public class AqlQueryDecorator {

    private DecorationStrategy[] strategies;

    public AqlQueryDecorator(DecorationStrategy... strategies) {
        this.strategies = strategies;
    }

    public void decorate(AqlQuery aqlQuery) {
        for (DecorationStrategy strategy : strategies) {
            strategy.decorate(aqlQuery);
        }
    }
}
