package org.artifactory.storage.db.aql.sql.builder.query.sql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQueryElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.Criteria;
import org.artifactory.storage.db.aql.sql.builder.query.sql.type.*;
import org.artifactory.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * The Class converts AqlQuery into sql query
 * Basically the query is ANSI SQL except the limit and the offset
 * @author Gidi Shabat
 */
public class SqlQueryBuilder {
    private Map<AqlDomainEnum, BasicSqlGenerator> sqlGeneratorMap;

    public SqlQueryBuilder() {
        sqlGeneratorMap = Maps.newHashMap();
        sqlGeneratorMap.put(AqlDomainEnum.artifacts, new ArtifactsSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.full_artifacts, new ArtifactsSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.properties, new PropertiesSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.archive, new ArchiveSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.statistics, new StatisticsSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.build_artifacts, new BuildArtifactSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.build_dependencies, new BuildDependenciesSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.build_module, new BuildModuleSqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.build_props, new BuildPropertySqlGenerator());
        sqlGeneratorMap.put(AqlDomainEnum.builds, new BuildSqlGenerator());
    }

    public SqlQuery buildQuery(AqlQuery aqlQuery) throws AqlException {
        SqlQuery sqlQuery = new SqlQuery();
        AqlDomainEnum domainEnum = aqlQuery.getDomain();
        BasicSqlGenerator generator = sqlGeneratorMap.get(domainEnum);
        generateSqlQuery(aqlQuery, generator, sqlQuery);
        sqlQuery.setResultFields(aqlQuery.getResultFields());
        sqlQuery.setLimit(aqlQuery.getLimit());
        return sqlQuery;
    }

    private void generateSqlQuery(AqlQuery aqlQuery, BasicSqlGenerator handler, SqlQuery query) throws AqlException {
        // Generate the result part of the query
        insertResultFieldsIntoQuery(aqlQuery, handler, query);
        // Generate the from part of the query
        insertTablesIntoQuery(aqlQuery, handler, query);
        // Add where clause if needed
        insertWhereClauseIntoQuery(aqlQuery, query);
        // Generate the filter part of the query
        insertFilterIntoQuery(aqlQuery, handler, query);
        // Generate the sort part of the query
        insertSortIntoQuery(aqlQuery, handler, query);
        // Generate limit part of the query
        insertLimitIntoQuery(aqlQuery, query);
    }



    private void insertWhereClauseIntoQuery(AqlQuery aqlQuery, SqlQuery query) {
        boolean whereClause = SqlQueryBuilder.isWhereClauseExist(aqlQuery);
        query.updateWhereClause(whereClause);
    }

    private void insertTablesIntoQuery(AqlQuery aqlQuery, BasicSqlGenerator handler, SqlQuery query) {
        query.updateTables(handler.tables(aqlQuery));
    }

    private void insertResultFieldsIntoQuery(AqlQuery aqlQuery, BasicSqlGenerator handler, SqlQuery query) {
        query.updateResultFields(handler.results(aqlQuery));
    }

    private void insertSortIntoQuery(AqlQuery aqlQuery, BasicSqlGenerator handler, SqlQuery query) {
        query.updateSort(handler.sort(aqlQuery));
    }

    private void insertLimitIntoQuery(AqlQuery aqlQuery, SqlQuery query) {
        int limit = aqlQuery.getLimit();
        if (limit > 0 && limit < Integer.MAX_VALUE) {
            DbService dbService = ContextHelper.get().beanForType(DbService.class);
            DbType databaseType = dbService.getDatabaseType();
            if (databaseType == DbType.ORACLE) {
                query.updateOracleLimit(limit);
            } else if (databaseType == DbType.POSTGRESQL) {
                query.updatePostgreSqlLimit(limit);
            } else if (databaseType == DbType.MSSQL) {
                query.updateMsSqlLimit(limit);
            } else if (databaseType == DbType.MYSQL) {
                query.updateMySqlLimit(limit);
            } else if (databaseType == DbType.DERBY) {
                query.updateDerbyLimit(limit);
            }
        }
    }

    private void insertFilterIntoQuery(AqlQuery aqlQuery, BasicSqlGenerator handler, SqlQuery query) {
        boolean whereClause = SqlQueryBuilder.isWhereClauseExist(aqlQuery);
        if (whereClause) {
            Pair<String, List<Object>> filter = handler.conditions(aqlQuery);
            query.updateFilter(filter.getFirst(), filter.getSecond());
        } else {
            query.updateFilter("", Lists.newArrayList());
        }
    }

    private static boolean isWhereClauseExist(AqlQuery aqlQuery) {
        List<AqlQueryElement> elements = aqlQuery.getAqlElements();
        for (AqlQueryElement element : elements) {
            if (element instanceof Criteria) {
                return true;
            }
        }
        return false;
    }
}
