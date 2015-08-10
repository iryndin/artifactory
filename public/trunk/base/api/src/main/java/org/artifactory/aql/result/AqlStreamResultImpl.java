package org.artifactory.aql.result;

import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlItemTypeEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gidi Shabat
 *         The class converts the AqlLazyResult to STREAM Aql Json result
 */
public class AqlStreamResultImpl extends AqlRestResult implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(AqlStreamResultImpl.class);
    private static final String QUERY_PREFIX = "\n{\n\"results\" : [ ";
    private static final String NUMBER_OF_ROWS = "<NUMBER_OF_ROWS>";
    private static final String QUERY_POSTFIX = " ],\n\"range\" : " + NUMBER_OF_ROWS + "\n}\n";
    private final long limit;
    private final long offset;
    private final Map<AqlFieldEnum, String> dbFieldNames;
    private final AqlDomainEnum domain;
    private int rowsCount;
    private Buffer buffer = new Buffer();
    private boolean firstRow = true;
    private boolean ended = false;
    private ResultSet resultSet;
    private List<DomainSensitiveField> resultFields;

    public AqlStreamResultImpl(AqlLazyResult lazyResult) {
        super(lazyResult.getPermissionProvider());
        this.resultSet = lazyResult.getResultSet();
        this.resultFields = lazyResult.getFields();
        this.limit = lazyResult.getLimit();
        this.offset = lazyResult.getOffset();
        this.domain = lazyResult.getDomain();
        dbFieldNames = lazyResult.getDbFieldNames();
        buffer.push(QUERY_PREFIX.getBytes());
    }

    @Override
    public void close() throws IOException {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.trace("Could not close JDBC result set", e);
            } catch (Exception e) {
                log.trace("Unexpected exception when closing JDBC result set", e);
            }
        }
    }

    /**
     * Reads Single row from The Json result
     * The method return null to signal end of stream
     *
     * @return Json row in byte array
     * @throws IOException
     */
    @Override
    public byte[] read() throws IOException {
        // Use the data in the buffer, reloading  the buffer is allowed only if it is empty
        if (!buffer.isEmpty()) {
            return buffer.getData();
        }
        // Fill the buffer from result-set
        if (getNewRowFromDb()) {
            rowsCount++;
            pushRowToBuffer();
            return buffer.getData();
        }
        // Fill the buffer from post fix
        if (!ended) {
            appendEndSection();
            return buffer.getData();
        }
        return null;
    }

    /**
     * Load the next row from ResultSet
     *
     * @return true if a new row is available and the current rows count is less than the limit
     */
    private boolean getNewRowFromDb() {
        try {
            if (rowsCount < limit) {
                boolean next = resultSet.next();
                while (next) {
                    if (canRead(domain, resultSet)) {
                        return true;
                    }
                    next = resultSet.next();
                }
            }
            return false;
        } catch (SQLException e) {
            log.error("An error has occur during AQL result stream: Stopping the stream.", e);
            return false;
        }
    }

    /**
     * Reads single row from database, convert it into Json and push the Json row to the buffer.
     *
     * @throws IOException
     */
    private void pushRowToBuffer() throws IOException {
        try {
            StringBuilder row = new StringBuilder();
            String tempRow = appendRowFields();
            if (tempRow != null) {
                appendRowPrefix(row);
                row.append(tempRow);
            }
            String stringRow = row.toString();
            buffer.push(stringRow.getBytes());
        } catch (SQLException e) {
            throw new IOException("An error has occur during AQL result stream: Stopping the stream.", e);
        }
    }

    private void appendEndSection() {
        try {
            if (!ended) {
                String range = generateRangeJson();
                String summary = StringUtils.replace(QUERY_POSTFIX, NUMBER_OF_ROWS, "" + range);
                buffer.push(summary.getBytes());
                ended = true;
            }
        } catch (IOException e) {
            log.error("Failed to generate Aql result summery.", e);
        }
    }

    private void appendRowPrefix(StringBuilder row) {
        if (firstRow) {
            firstRow = false;
        } else {
            row.append(",");
        }
    }

    /**
     * Reads single row By the ResultSet from the database and convert it into Json row
     *
     */
    private String appendRowFields() throws IOException, SQLException {
        Iterator<DomainSensitiveField> iterator = resultFields.iterator();
        Row row = new Row();
        while (iterator.hasNext()) {
            DomainSensitiveField field = iterator.next();
            AqlFieldEnum fieldEnum = field.getField();
            String dbFieldName = dbFieldNames.get(fieldEnum);
            switch (fieldEnum.type) {
                case date: {
                    Long valueLong = resultSet.getLong(dbFieldName);
                    String value = valueLong == 0 ? null : ISODateTimeFormat.dateTime().print(valueLong);
                    row.put(fieldEnum.name(), value);
                    break;
                }
                case longInt: {
                    long value = resultSet.getLong(dbFieldName);
                    row.put(fieldEnum.name(), value);
                    break;
                }
                case integer: {
                    int value = resultSet.getInt(dbFieldName);
                    row.put(fieldEnum.name(), value);
                    break;
                }
                case string: {
                    String value = resultSet.getString(dbFieldName);
                    row.put(fieldEnum.name(), value);
                    break;
                }
                case itemType: {
                    int type = resultSet.getInt(dbFieldName);
                    AqlItemTypeEnum aqlItemTypeEnum = AqlItemTypeEnum.fromTypes(type);
                    row.put(fieldEnum.name(), aqlItemTypeEnum);
                    break;
                }
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.setVisibility(JsonMethod.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(row);
    }

    private String generateRangeJson() throws IOException {
        Range range = new Range(offset, rowsCount, rowsCount, limit);
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.setVisibility(JsonMethod.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(range);
    }

    private class Buffer {
        private byte[] buffer;

        public void push(byte[] bytes) {
            buffer = bytes;
        }

        public byte[] getData() {
            byte[] temp = buffer;
            buffer = null;
            return temp;
        }

        public boolean isEmpty() {
            return buffer == null;
        }
    }
}