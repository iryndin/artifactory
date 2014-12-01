package org.artifactory.storage.db.aql.sql.result;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.result.AqlQueryStreamResultIfc;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Streaming query result
 *
 * @author Gidi Shabat
 */
public class AqlQueryStreamResult extends InputStream implements AqlQueryStreamResultIfc {
    private static final Logger log = LoggerFactory.getLogger(AqlQueryStreamResult.class);
    private static final String RESULT_PREFIX = "\n" +
            "###############################################################\n" +
            "####                 AND THE WINNERS ARE                   ####\n" +
            "###############################################################\n" +
            "{\n\"results\":[";
    private static final String NUMBER_OF_ROWS = "<NUMBER_OF_ROWS>";
    private static final String RESULT_POSTFIX = "]\n}\n" +
            "\nAql has been finished successfully with: " + NUMBER_OF_ROWS + " rows.\n\n";
    private final int limit;
    private int rowsCount;
    private Buffer buffer = new Buffer();
    private boolean firstRow = true;
    private boolean ended = false;
    private ResultSet resultSet;
    private List<AqlField> resultFields;

    public AqlQueryStreamResult(ResultSet resultSet, SqlQuery sqlQuery) {
        this.resultSet = resultSet;
        this.resultFields = sqlQuery.getResultFields();
        this.limit = sqlQuery.getLimit();
        buffer.push(RESULT_PREFIX.getBytes());
    }

    @Override
    public void close() throws IOException {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            log.error("An error has occur during AQL result stream close process.", e);
        }
    }

    @Override
    public int read() throws IOException {
        // Use the data in the buffer, reloading  the buffer is allowed only if it is empty
        if (!buffer.isEmpty()) {
            return buffer.getByte();
        }
        // Fill the buffer from result-set
        if (getNewRowFromDb()) {
            rowsCount++;
            pushRowToBuffer();
            return buffer.getByte();
        }
        // Fill the buffer from post fix
        if (!ended) {
            appendEndPart();
            return buffer.getByte();
        }
        return -1;
    }

    private boolean getNewRowFromDb() {
        try {
            if (rowsCount < limit) {
                return resultSet.next();
            }
            return false;
        } catch (SQLException e) {
            log.error("An error has occur during AQL result stream: Stopping the stream.", e);
            return false;
        }
    }

    private void pushRowToBuffer() throws IOException {
        try {
            StringBuilder row = new StringBuilder();
            StringBuilder tempRow = appendRowFields();
            if (tempRow != null) {
                appendRowPrefix(row);
                row.append(tempRow);
                appendRowPostfix(row);
            }
            String stringRow = row.toString();
            buffer.push(stringRow.getBytes());
        } catch (SQLException e) {
            throw new IOException("An error has occur during AQL result stream: Stopping the stream.", e);
        }
    }

    private void appendEndPart() {
        if (!ended) {
            String endMessage = StringUtils.replace(RESULT_POSTFIX, NUMBER_OF_ROWS, "" + rowsCount);
            buffer.push(endMessage.getBytes());
            ended = true;
        }
    }

    private void appendRowPostfix(StringBuilder row) {
        row.append("}");
    }

    private void appendRowPrefix(StringBuilder row) {
        if (firstRow) {
            row.append("\n\t{");
            firstRow = false;
        } else {
            row.append(",\n\t{");
        }
    }

    private StringBuilder appendRowFields() throws SQLException {
        Iterator<AqlField> iterator = resultFields.iterator();
        Map<AqlField, Object> map = Maps.newHashMap();
        StringBuilder row = null;
        boolean resultSetIsReady = true;
        while (row == null && resultSetIsReady) {
            row = new StringBuilder();
            while (iterator.hasNext()) {
                AqlField field = iterator.next();
                AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(field);
                switch (field.type) {
                    case date: {
                        Long value = resultSet.getLong(extension.tableField.name());
                        map.put(field, value == 0 ? null : new Date(value));
                        break;
                    }
                    case longInt: {
                        long value = resultSet.getLong(extension.tableField.name());
                        map.put(field, value);
                        break;
                    }
                    case integer: {
                        int value = resultSet.getInt(extension.tableField.name());
                        map.put(field, value);
                        break;
                    }
                    case string: {
                        String value = resultSet.getString(extension.tableField.name());
                        map.put(field, value);
                        break;
                    }
                }
            }
            // Remove invalid rows (rows with not null fields with null values)
            boolean invalidRow = false;
            for (AqlField field : map.keySet()) {
                Object value = map.get(field);
                AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(field);
                if (value == null && !extension.isNullable()) {
                    invalidRow = true;
                    break;
                }
            }
            // if valid row then create row
            if (!invalidRow) {
                Iterator<AqlField> mapIterator = map.keySet().iterator();
                while (mapIterator.hasNext()) {
                    AqlField fieldsExtentionEnum = mapIterator.next();
                    Object value = map.get(fieldsExtentionEnum);
                    row.append("\"").append(fieldsExtentionEnum.name()).append("\"").append(":").append("\"").append(
                            value).append("\"");
                    if (mapIterator.hasNext()) {
                        row.append(",");
                    }
                }

            } else {
                row = null;
                resultSetIsReady = resultSet.next();
            }
        }
        return row;
    }

    private class Buffer {
        private byte[] buffer;
        private int pointer = 0;
        private int max = 0;

        public void push(byte[] bytes) {
            if (pointer != max) {
                throw new IllegalStateException("Invalid state, push is allowed only if buffer is empty ");
            }
            buffer = bytes;
            pointer = 0;
            max = bytes.length;
        }

        public byte getByte() {
            if (pointer < max && pointer >= 0) {
                return buffer[pointer++];
            } else {
                log.error("Buffer overload for pointer {} buffer size {}", pointer, max);
                return -1;
            }
        }

        public boolean isEmpty() {
            return pointer >= max;
        }
    }

}
