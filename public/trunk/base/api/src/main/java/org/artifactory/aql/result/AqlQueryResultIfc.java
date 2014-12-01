package org.artifactory.aql.result;

import org.artifactory.aql.result.rows.AqlRowResult;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public interface AqlQueryResultIfc<T extends AqlRowResult> {
    int getSize();

    T getResult(int j);

    List<T> getResults();
}
