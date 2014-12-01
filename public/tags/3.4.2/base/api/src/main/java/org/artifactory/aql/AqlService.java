package org.artifactory.aql;

import org.artifactory.aql.api.Aql;
import org.artifactory.aql.result.AqlQueryResultIfc;
import org.artifactory.aql.result.AqlQueryStreamResultIfc;
import org.artifactory.aql.result.rows.AqlRowResult;

/**
 * @author Gidi Shabat
 */
public interface AqlService {

    /**
     * Parse the AQL query,
     * convert the parser result into Aql query,
     * convert the Aql query to sql query
     * and finally execute the query lazy
     */
    AqlQueryStreamResultIfc executeQueryLazy(String query);

    /**
     * Parse the AQL query,
     * convert the parser result into AqlApi query,
     * convert the AqlApi query to sql query
     * and finally execute the query eagerly
     */
    AqlQueryResultIfc executeQueryEager(String query);

    /**
     * Converts the AQL API QUERY into aqlApi query,
     * then convert the aqlApi query into SQL query,
     * and finally execute the query eagerly
     */
    <T extends AqlRowResult> AqlQueryResultIfc<T> executeQueryEager(Aql query);

    AqlQueryStreamResultIfc executeQueryLazy(Aql query);

}
