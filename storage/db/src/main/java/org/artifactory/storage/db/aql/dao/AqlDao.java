package org.artifactory.storage.db.aql.dao;

import org.artifactory.aql.AqlException;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.artifactory.storage.db.aql.sql.result.AqlQueryResult;
import org.artifactory.storage.db.aql.sql.result.AqlQueryStreamResult;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Gidi Shabat
 */
@Repository
public class AqlDao extends BaseDao {
    @Autowired
    public AqlDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    /**
     * Execute the AQL query ant fetch the results eagerly
     * Note that it is recommended to use the executeQueryLazy which consumes less memory and allow minimal waiting time.
     */
    public AqlQueryResult executeQueryEager(SqlQuery sqlQuery) {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect(sqlQuery.getQueryString(), sqlQuery.getQueryParams());
            AqlQueryResult aqlQueryResult = new AqlQueryResult(resultSet, sqlQuery);
            return aqlQueryResult;
        } catch (Exception e) {
            throw new AqlException("Failed to execute the following sql query" + sqlQuery, e);
        } finally {
            DbUtils.close(resultSet);
        }
    }

    /**
     * Execute the AQL query and fetch the results lazy
     * Unlike the Eager mode which loads all the results into memory and only then return it to the caller.
     * The lazy mode dynamically fills the result into stream, actually the AqlQueryStreamResult role is is kind of
     * broker between the database and the client
     */
    public AqlQueryStreamResult executeQueryLazy(SqlQuery sqlQuery) {
        ResultSet resultSet;
        try {
            resultSet = jdbcHelper.executeSelect(sqlQuery.getQueryString(), sqlQuery.getQueryParams());
        } catch (SQLException e) {
            throw new AqlException("Failed to execute the following sql query" + sqlQuery, e);
        }
        AqlQueryStreamResult aqlQueryResult = new AqlQueryStreamResult(resultSet, sqlQuery);
        return aqlQueryResult;
    }
}
