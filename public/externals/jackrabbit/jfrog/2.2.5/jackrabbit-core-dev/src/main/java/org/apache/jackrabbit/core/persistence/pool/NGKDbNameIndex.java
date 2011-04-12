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
package org.apache.jackrabbit.core.persistence.pool;

import java.sql.SQLException;
import java.sql.Statement;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;

/**
 * Same as {@link DbNameIndex} but does not make use of the
 * {@link Statement#RETURN_GENERATED_KEYS} feature as it might not be provided
 * by the underlying database (e.g. oracle).
 */
public class NGKDbNameIndex extends DbNameIndex {

    /**
     * Creates a new index that is stored in a db.
     * @param conHelper the {@link ConnectionHelper}
     * @param schemaObjectPrefix the prefix for table names
     * @throws SQLException if the statements cannot be prepared.
     */
    public NGKDbNameIndex(ConnectionHelper conHelper, String schemaObjectPrefix)
            throws SQLException {
        super(conHelper, schemaObjectPrefix);
    }

    /**
     * {@inheritDoc}
     */
    protected void init(String schemaObjectPrefix)
            throws SQLException {
        nameSelectSQL = "select NAME from " + schemaObjectPrefix + "NAMES where ID = ?";
        indexSelectSQL = "select ID from " + schemaObjectPrefix + "NAMES where NAME = ?";
        nameInsertSQL = "insert into " + schemaObjectPrefix + "NAMES (NAME) values (?)";
    }

    /**
     * Inserts a string into the database and returns the new index.
     * <p/>
     * Instead of using the {@link Statement#RETURN_GENERATED_KEYS} feature, the
     * newly inserted index is retrieved by a 2nd select statement.
     *
     * @param string the string to insert
     * @return the new index.
     */
    protected int insertString(String string) {
        // assert index does not exist
        try {
            conHelper.exec(nameInsertSQL, new Object[] { string });
        } catch (Exception e) {
            IllegalStateException ise = new IllegalStateException(
                    "Unable to insert index for string: " + string);
            ise.initCause(e);
            throw ise;
        }
        return getIndex(string);
    }
}
