package org.artifactory.aql.result;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlItemTypeEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.common.ConstantValues;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Gidi Shabat
 *         The class converts the AqlLazyResult to in-memory Aql Json result
 *         The Max number of rows allowed by this result is the actually artifactory searchUserQueryLimit:
 *         (ConstantValues.searchUserQueryLimit)
 */
public class AqlJsonResult extends AqlRestResult implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(AqlJsonResult.class);
    private final ResultSet resultSet;
    private final List<DomainSensitiveField> fields;
    private final Map<AqlFieldEnum, String> dbFieldNames;
    private final ResultHolder resultHolder;
    private final long realLimit;
    private final AqlDomainEnum domain;
    private byte[] out;

    public AqlJsonResult(AqlLazyResult lazyResult) {
        super(lazyResult.getPermissionProvider());
        this.resultSet = lazyResult.getResultSet();
        this.fields = lazyResult.getFields();
        this.dbFieldNames = lazyResult.getDbFieldNames();
        this.resultHolder = new ResultHolder();
        this.domain = lazyResult.getDomain();
        this.realLimit = Math.min(lazyResult.getLimit(), ConstantValues.searchUserQueryLimit.getLong());
        inflate();
    }

    /**
     * Read the ResultSet from db and converts the result into Json rows
     */
    private void inflate() {
        try {
            while (resultSet.next()) {
                Row row = new Row();
                for (DomainSensitiveField field : fields) {
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
                // Merge the rows or add new row
                resultHolder.mergeOrAdd(row);
            }
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.getSerializationConfig().withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
                mapper.setVisibility(JsonMethod.ALL, JsonAutoDetect.Visibility.NONE);
                mapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                Collection<Row> values = resultHolder.map.values();
                long count = resultHolder.map.values().size();
                ResultContainer resultContainer = new ResultContainer(values, 0, count, realLimit);
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultContainer);
                out = json.getBytes();
                arrayOutputStream.close();
            } catch (Exception e) {
                log.error("Failed to convert Aql Result to JSON");
            }
        } catch (Exception e) {
            log.error("Failed to fetch Aql result: ", e);
        }
    }

    @Override
    public byte[] read() {
        try {
            if (out != null) {
                byte[] array = out;
                out = null;
                return array;
            }
            return null;
        } finally {
            close();
        }
    }

    @Override
    public void close() {
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
     * Holds the row result by key to allow rows addition and merge properties
     */
    private class ResultHolder {
        protected Map<String, Row> map = Maps.newHashMap();

        public void mergeOrAdd(Row newRow) throws Exception {
            String propertyKey = newRow.propertyKey;
            String propertyValue = newRow.propertyValue;
            // Remove the properties data from the fields and merge it in the properties list
            newRow.propertyKey = null;
            newRow.propertyValue = null;
            String key = newRow.getKey();
            Row targetRow = map.get(key);
            if (targetRow == null) {
                // Add the row to the map only if map size is less than the limit and user have read authorization
                if (map.size() < realLimit) {
                    if (canRead(domain, newRow.itemRepo, newRow.itemPath, newRow.itemName)) {
                        targetRow = newRow;
                        map.put(key, targetRow);
                        updateProperties(propertyKey, propertyValue, targetRow);
                    }
                }
            } else {
                // Merge properties in existing row.
                updateProperties(propertyKey, propertyValue, targetRow);
            }
        }

        private void updateProperties(String propertyKey, String propertyValue, Row targetRow) throws Exception {
            if (propertyKey != null) {
                List<Property> properties = getLazyProperties(targetRow);
                Property property = new Property();
                properties.add(property);
                property.put(AqlFieldEnum.propertyKey.name(), propertyKey);
                if (propertyValue != null) {
                    property.put(AqlFieldEnum.propertyValue.name(), propertyValue);
                }
                return;
            }
            if (propertyValue != null) {
                List<Property> properties = getLazyProperties(targetRow);
                Property property = new Property();
                properties.add(property);
                property.put(AqlFieldEnum.propertyValue.name(), propertyValue);
            }
        }
    }

    private List<Property> getLazyProperties(Row targetRow) throws Exception {
        List<Property> properties = targetRow.properties;
        if (properties == null) {
            properties = Lists.newArrayList();
            targetRow.put(AqlDomainEnum.properties.name(), properties);
        }
        return properties;
    }

    private class ResultContainer {
        public List<Row> results;
        public Range range;

        public ResultContainer(Collection<Row> values, long start, long end, long limited) {
            results = Lists.newArrayList(values);
            range = new Range(start, end, limited);
        }


    }
}
