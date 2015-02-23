package org.artifactory.storage.db.aql.sql.builder.query.aql;

/**
 * @author Gidi Shabat
 */
public class FlatAqlElement implements AqlQueryElement {

    @Override
    public boolean isOperator() {
        return false;
    }
}
