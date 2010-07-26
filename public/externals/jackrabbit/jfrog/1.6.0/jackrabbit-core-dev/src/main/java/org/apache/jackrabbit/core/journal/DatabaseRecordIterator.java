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
package org.apache.jackrabbit.core.journal;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * RecordIterator interface.
 */
class DatabaseRecordIterator implements RecordIterator {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(DatabaseJournal.class);

    /**
     * Underlying result set.
     */
    private final ResultSet rs;

    /**
     * Namespace resolver.
     */
    private final NamespaceResolver resolver;

    /**
     * Name and Path resolver.
     */
    private final NamePathResolver npResolver;

    /**
     * Current record.
     */
    private ReadRecord record;

    /**
     * Last record returned.
     */
    private ReadRecord lastRecord;

    /**
     * Flag indicating whether EOF was reached.
     */
    private boolean isEOF;

    /**
     * Create a new instance of this class.
     */
    public DatabaseRecordIterator(ResultSet rs, NamespaceResolver resolver, NamePathResolver npResolver) {
        this.rs = rs;
        this.resolver = resolver;
        this.npResolver = npResolver;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        try {
            if (!isEOF && record == null) {
                fetchRecord();
            }
            return !isEOF;
        } catch (SQLException e) {
            String msg = "Error while moving to next record.";
            log.error(msg, e);
            return false;
        }
    }

    /**
     * Return the next record. If there are no more recors, throws
     * a <code>NoSuchElementException</code>. If an error occurs,
     * throws a <code>JournalException</code>.
     *
     * @return next record
     * @throws java.util.NoSuchElementException if there are no more records
     * @throws JournalException if another error occurs
     */
    public Record nextRecord() throws NoSuchElementException, JournalException {
        if (!hasNext()) {
            String msg = "No current record.";
            throw new NoSuchElementException(msg);
        }
        close(lastRecord);
        lastRecord = record;
        record = null;

        return lastRecord;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        if (lastRecord != null) {
            close(lastRecord);
            lastRecord = null;
        }
        try {
            rs.close();
        } catch (SQLException e) {
            String msg = "Error while closing result set: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Fetch the next record.
     */
    private void fetchRecord() throws SQLException {
        if (rs.next()) {
            long revision = rs.getLong(1);
            String journalId = rs.getString(2);
            String producerId = rs.getString(3);
            DataInputStream dataIn = new DataInputStream(rs.getBinaryStream(4));
            record = new ReadRecord(journalId, producerId, revision, dataIn, 0, resolver, npResolver);
        } else {
            isEOF = true;
        }
    }

    /**
     * Close a record.
     *
     * @param record record
     */
    private static void close(ReadRecord record) {
        if (record != null) {
            try {
                record.close();
            } catch (IOException e) {
                String msg = "Error while closing record.";
                log.warn(msg, e);
            }
        }
    }

}
