package org.artifactory.storage.db.aql.service;

import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.Aql;
import org.artifactory.aql.result.AqlQueryResultIfc;
import org.artifactory.aql.result.AqlQueryStreamResultIfc;
import org.artifactory.aql.result.rows.AqlRowResult;
import org.artifactory.storage.db.aql.dao.AqlDao;
import org.artifactory.storage.db.aql.parser.AqlParser;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlApiToAqlAdapter;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;
import org.artifactory.storage.db.aql.sql.builder.query.aql.ParserToAqlAdapter;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQueryBuilder;
import org.artifactory.storage.db.aql.sql.result.AqlQueryResult;
import org.artifactory.storage.db.aql.sql.result.AqlQueryStreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Execute the Aql queries by processing the three Aql steps one after the other:
 * Step 1 Convert the AqlApi or the parser result into AqlQuery.
 * Step 2 Convert the AqlQuery into SqlQuery.
 * Step 3 Execute the SqlQuery and return the results.
 *
 * @author Gidi Shabat
 */
@Service
public class AqlServiceImpl implements AqlService {
    private static final Logger log = LoggerFactory.getLogger(AqlServiceImpl.class);

    @Autowired
    private AqlDao aqlDao;

    private AqlParser parser;
    private ParserToAqlAdapter parserToAqlAdapter;
    private AqlApiToAqlAdapter apiToAqlAdapter;
    private SqlQueryBuilder sqlQueryBuilder;

    @PostConstruct
    private void initDb() throws Exception {
        // The parser is constructed by many internal elements therefore we create it once and then reuse it.
        // Please note that it doesn't really have state therefore we can use it simultaneously
        // TODO init the parser eagerly here not lazy
        parser = new AqlParser();
        parserToAqlAdapter = new ParserToAqlAdapter();
        apiToAqlAdapter = new AqlApiToAqlAdapter();
        sqlQueryBuilder = new SqlQueryBuilder();
    }

    /**
     * Converts the Json query into SQL query and executes the query eagerly
     */
    @Override
    public AqlQueryResultIfc executeQueryEager(String query) {
        log.debug("Processing textual AqlApi query: {}", query);
        ParserElementResultContainer parserResult = parser.parse(query);
        return executeQueryEager(parserResult);
    }

    /**
     * Converts the Json query into SQL query and executes the query lazy
     */
    @Override
    public AqlQueryStreamResultIfc executeQueryLazy(String query) {
        log.debug("Processing textual AqlApi query: {}", query);
        ParserElementResultContainer parserResult = parser.parse(query);
        return executeQueryLazy(parserResult);
    }

    /**
     * Converts the API's AqlApi query into SQL query and executes the query eagerly
     */
    @Override
    public <T extends AqlRowResult> AqlQueryResultIfc<T> executeQueryEager(Aql aql) {
        log.debug("Processing API AqlApi query");
        AqlQuery aqlQuery = apiToAqlAdapter.toAqlModel(aql);
        return (AqlQueryResult<T>) getAqlQueryResult(aqlQuery);
    }

    @Override
    public AqlQueryStreamResultIfc executeQueryLazy(Aql aql) {
        log.debug("Processing API AqlApi query");
        AqlQuery aqlQuery = apiToAqlAdapter.toAqlModel(aql);
        return getAqlQueryStreamResult(aqlQuery);
    }

    /**
     * Converts the parser elements into AqlApi query, convert the AqlApi query to sql and executes the query eagerly
     */
    private AqlQueryResultIfc executeQueryEager(ParserElementResultContainer parserResult) {
        log.trace("Converting the parser result into AqlApi query");
        AqlQuery aqlQuery = parserToAqlAdapter.toAqlModel(parserResult);
        log.trace("Successfully finished to convert the parser result into AqlApi query");
        AqlQueryResultIfc aqlQueryResult = getAqlQueryResult(aqlQuery);
        return aqlQueryResult;
    }

    /**
     * Converts the parser elements into AqlApi query and executes the query lazy
     */
    private AqlQueryStreamResultIfc executeQueryLazy(ParserElementResultContainer parserResult) {
        log.trace("Converting the parser result into AqlApi query");
        AqlQuery aqlQuery = parserToAqlAdapter.toAqlModel(parserResult);
        log.trace("Successfully finished to convert the parser result into AqlApi query");
        return getAqlQueryStreamResult(aqlQuery);
    }

    /**
     * Converts the AqlApi query into SQL query and executes the query eagerly
     */
    private AqlQueryResultIfc getAqlQueryResult(AqlQuery aqlQuery) {
        log.trace("Converting the AqlApi query into SQL query: {}", aqlQuery);
        SqlQuery sqlQuery = sqlQueryBuilder.buildQuery(aqlQuery);
        log.trace("Successfully finished to convert the parser result into the following SQL query '{}'", sqlQuery);
        log.debug("processing the following SQL query: {}", sqlQuery);
        AqlQueryResult aqlQueryResult = aqlDao.executeQueryEager(sqlQuery);
        log.debug("Successfully finished to process SQL query with the following size: {}", aqlQueryResult.getSize());
        return aqlQueryResult;
    }

    private AqlQueryStreamResultIfc getAqlQueryStreamResult(AqlQuery aqlQuery) {
        log.trace("Converting the AqlApi query into SQL qury: {}", aqlQuery);
        SqlQuery sqlQuery = sqlQueryBuilder.buildQuery(aqlQuery);
        log.trace("Successfully finished to convert the parser result into the following SQL query '{}'", sqlQuery);
        log.debug("processing the following SQL query: {}", sqlQuery);
        AqlQueryStreamResult aqlQueryStreamResult = aqlDao.executeQueryLazy(sqlQuery);
        log.debug("Successfully finished to process SQL query (lazy)");
        return aqlQueryStreamResult;
    }

}