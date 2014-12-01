package org.artifactory.storage.db.aql.sql.builder.query.sql;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlField;

import java.util.List;

/**
 * The class represent Sql query, it also contains some simple convenient methods that helps to build the query
 *
 * @author Gidi Shabat
 */
public class SqlQuery {
    private static final String FIELDS = "<FIELDS>".intern();
    private static final String TABLES = "<TABLES>";
    private static final String FILTERS = "<FILTERS>";
    private static final String WHERE = "<WHERE>";
    private static final String SORT = "<SORT>";
    private static final String SELECT_DISTINCT = "select distinct";
    private static final String QUERY_TEMPLATE = SELECT_DISTINCT + FIELDS + "from" + TABLES + WHERE + FILTERS + SORT;
    private String query = QUERY_TEMPLATE;
    List<Object> params = Lists.newArrayList();
    private List<AqlField> resultFields;
    private int limit;

    public void updateResultFields(String results) {
        query = query.replace(FIELDS, results);
    }

    public void updateFilter(String filters, List<Object> params) {
        query = query.replace(FILTERS, filters);
        this.params = params;
    }

    public void updateSort(String sort) {
        query = query.replace(SORT, sort);
    }

    public void updateTables(String tables) {
        query = query.replace(TABLES, tables);
    }

    public void updateWhereClause(boolean whereClauseExist) {
        query = query.replace(WHERE, whereClauseExist ? " where" : "");
    }

    public void updateOracleLimit(int limit) {
        query = "select * from (" + query + ") where ROWNUM <= " + limit;
    }

    public void updateMySqlLimit(int limit) {
        query = query + " limit " + limit;
    }

    public void updateDerbyLimit(int limit) {
        query = query + " FETCH FIRST " + limit + " ROWS ONLY";

    }

    public void updatePostgreSqlLimit(int limit) {
        query = query + " limit " + limit;
    }

    public void updateMsSqlLimit(int limit) {
        query=query.replaceFirst(SELECT_DISTINCT, "select distinct top " + limit);
    }

    public Object[] getQueryParams() {
        Object[] objects = new Object[params.size()];
        return params.toArray(objects);
    }

    public String getQueryString() {
        return query;
    }

    @Override
    public String toString() {
        return "SqlQuery{" +
                "query='" + query + '\'' +
                ", params=" + params +
                '}';
    }

    public List<AqlField> getResultFields() {
        return resultFields;
    }

    public void setResultFields(List<AqlField> resultFields) {
        this.resultFields = resultFields;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
