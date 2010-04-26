/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.version.VersionException;
import java.text.NumberFormat;

/**
 * Provides the default implementation for a JCR query.
 */
public class QueryImpl extends AbstractQueryImpl {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryImpl.class);

    /**
     * The session of the user executing this query
     */
    protected SessionImpl session;

    /**
     * The query statement
     */
    protected String statement;

    /**
     * The syntax of the query statement
     */
    protected String language;

    /**
     * The actual query implementation that can be executed
     */
    protected ExecutableQuery query;

    /**
     * The node where this query is persisted. Only set when this is a persisted
     * query.
     */
    protected Node node;

    /**
     * The query handler for this query.
     */
    protected QueryHandler handler;

    /**
     * Flag indicating whether this query is initialized.
     */
    private boolean initialized = false;

    /**
     * The maximum result size
     */
    private long limit = -1;

    /**
     * The offset in the total result set
     */
    private long offset;

    /**
     * @inheritDoc
     */
    public void init(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     String statement,
                     String language,
                     Node node) throws InvalidQueryException {
        checkNotInitialized();
        this.session = session;
        this.statement = statement;
        this.language = language;
        this.handler = handler;
        this.node = node;
        this.query = handler.createExecutableQuery(session, itemMgr, statement, language);
        setInitialized();
    }

    /**
     * This method simply forwards the <code>execute</code> call to the
     * {@link ExecutableQuery} object returned by
     * {@link QueryHandler#createExecutableQuery}.
     * {@inheritDoc}
     */
    public QueryResult execute() throws RepositoryException {
        checkInitialized();
        long time = System.currentTimeMillis();
        QueryResult result = query.execute(offset, limit);
        if (log.isDebugEnabled()) {
            time = System.currentTimeMillis() - time;
            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            String seconds = format.format((double) time / 1000);
            log.debug("executed in " + seconds + " s. (" + statement + ")");
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getStatement() {
        checkInitialized();
        return statement;
    }

    /**
     * {@inheritDoc}
     */
    public String getLanguage() {
        checkInitialized();
        return language;
    }

    /**
     * {@inheritDoc}
     */
    public String getStoredQueryPath()
            throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        if (node == null) {
            throw new ItemNotFoundException("not a persistent query");
        }
        return node.getPath();
    }

    /**
     * {@inheritDoc}
     */
    public Node storeAsNode(String absPath)
            throws ItemExistsException,
            PathNotFoundException,
            VersionException,
            ConstraintViolationException,
            LockException,
            UnsupportedRepositoryOperationException,
            RepositoryException {

        checkInitialized();
        try {
            Path p = session.getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not an absolute path");
            }

            String relPath = session.getJCRPath(p).substring(1);
            Node queryNode = session.getRootNode().addNode(
                    relPath, session.getJCRName(NameConstants.NT_QUERY));
            // set properties
            queryNode.setProperty(session.getJCRName(NameConstants.JCR_LANGUAGE), language);
            queryNode.setProperty(session.getJCRName(NameConstants.JCR_STATEMENT), statement);
            node = queryNode;
            return node;
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getBindVariableNames() throws RepositoryException {
        Name[] names = query.getBindVariableNames();
        String[] strNames = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            strNames[i] = session.getJCRName(names[i]);
        }
        return strNames;
    }

    /**
     * Binds the given <code>value</code> to the variable named
     * <code>varName</code>.
     *
     * @param varName name of variable in query
     * @param value   value to bind
     * @throws IllegalArgumentException      if <code>varName</code> is not a
     *                                       valid variable in this query.
     * @throws javax.jcr.RepositoryException if an error occurs.
     */
    public void bindValue(String varName, Value value)
            throws IllegalArgumentException, RepositoryException {
        checkInitialized();
        try {
            query.bindValue(session.getQName(varName), value);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    /**
     * Sets the maximum size of the result set.
     *
     * @param limit new maximum size of the result set
     */
    public void setLimit(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negativ");
        }
        this.limit = limit;
    }

    /**
     * Sets the start offset of the result set.
     *
     * @param offset new start offset of the result set
     */
    public void setOffset(long offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negativ");
        }
        this.offset = offset;
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Sets the initialized flag.
     */
    protected void setInitialized() {
        initialized = true;
    }

    /**
     * Checks if this query is not yet initialized and throws an
     * <code>IllegalStateException</code> if it is already initialized.
     */
    protected void checkNotInitialized() {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
    }

    /**
     * Checks if this query is initialized and throws an
     * <code>IllegalStateException</code> if it is not yet initialized.
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

}
