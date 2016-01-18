package org.artifactory.storage.db.aql.dao;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.aql.result.AqlLazyResult;
import org.artifactory.common.ConstantValues;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.artifactory.storage.db.aql.sql.result.AqlEagerResultImpl;
import org.artifactory.storage.db.aql.sql.result.AqlLazyResultImpl;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.util.TxHelper;
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
    private DbService dbService;

    @Autowired
    public AqlDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    /**
     * Execute the AQL query ant fetch the results eagerly
     * Note that it is recommended to use the executeQueryLazy which consumes less memory and allow minimal waiting time.
     */
    public AqlEagerResultImpl executeQueryEager(SqlQuery sqlQuery) {
        ResultSet resultSet = null;
        try {
            if (allowReadCommitted()) {
                resultSet = jdbcHelper.executeSelect(sqlQuery.getQueryString(), sqlQuery.getQueryParams());
            } else {
                resultSet = jdbcHelper.executeSelect(sqlQuery.getQueryString(), true, sqlQuery.getQueryParams());
            }
            AqlEagerResultImpl aqlQueryResult = new AqlEagerResultImpl(resultSet, sqlQuery);
            return aqlQueryResult;
        } catch (Exception e) {
            throw new AqlException("Failed to execute the following sql query" + sqlQuery, e);
        } finally {
            DbUtils.close(resultSet);
        }
    }

    private boolean allowReadCommitted() {
        return dbService.getDatabaseType().equals(DbType.ORACLE) || dbService.getDatabaseType().equals(DbType.POSTGRESQL) ||
                ConstantValues.enableAqlReadCommitted.getBoolean() || TxHelper.isInTransaction();
    }

    /**
     * Execute the AQL query and fetch the results lazy
     * Unlike the Eager mode which loads all the results into memory and only then return it to the caller.
     * The lazy mode dynamically fills the result into stream, actually the AqlQueryStreamResult role is is kind of
     * broker between the database and the client
     */
    public AqlLazyResult executeQueryLazy(SqlQuery sqlQuery, AqlPermissionProvider aqlPermissionProvider) {
        ResultSet resultSet;
        try {
            if (allowReadCommitted()) {
                resultSet = jdbcHelper.executeSelect(sqlQuery.getQueryString(), sqlQuery.getQueryParams());
            } else {
                resultSet = jdbcHelper.executeSelect(sqlQuery.getQueryString(), true, sqlQuery.getQueryParams());
            }
        } catch (SQLException e) {
            throw new AqlException("Failed to execute the following sql query" + sqlQuery, e);
        }
        AqlLazyResult aqlQueryResult = new AqlLazyResultImpl(resultSet, sqlQuery, aqlPermissionProvider);
        return aqlQueryResult;
    }
}
