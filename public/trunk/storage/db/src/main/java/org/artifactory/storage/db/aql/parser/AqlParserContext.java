package org.artifactory.storage.db.aql.parser;

/**
 * This context is being used in case of parser syntax error it provide an accurate location of the syntax error.
 *
 * @author Gidi Shabat
 */
public class AqlParserContext {
    private String queryRemainder;

    /**
     * Each time a parser element success(matches sub string), the parser peels off the relevant sub string from the string query
     * and update the context with the remaining query
     * @param query
     */
    public void update(String query) {
        if (this.queryRemainder == null || query.length() < this.queryRemainder.length()) {
            this.queryRemainder = query;
        }
    }

    public String getQueryRemainder() {
        return queryRemainder;
    }
}
