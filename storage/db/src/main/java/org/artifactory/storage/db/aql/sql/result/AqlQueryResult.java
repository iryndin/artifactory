package org.artifactory.storage.db.aql.sql.result;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.result.AqlQueryResultIfc;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.result.rows.AqlRowResult;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * In-Memory query result
 *
 * @author Gidi Shabat
 */
public class AqlQueryResult<T extends AqlRowResult> implements AqlQueryResultIfc {
    private List<Map<AqlField, Object>> rows = Lists.newArrayList();

    public AqlQueryResult(ResultSet resultSet, SqlQuery sqlQuery) throws SQLException {
        int limit = sqlQuery.getLimit();
        while (resultSet.next() && rows.size() < limit) {
            Map<AqlField, Object> map = Maps.newHashMap();
            for (AqlField field : sqlQuery.getResultFields()) {
                AqlFieldExtensionEnum fieldsExtensionEnum = AqlFieldExtensionEnum.getExtensionFor(field);
                switch (field.type) {
                    case date: {
                        Long aLong = resultSet.getLong(fieldsExtensionEnum.tableField.name());
                        map.put(field, aLong == 0 ? null : new Date(aLong));
                        break;
                    }
                    case longInt: {
                        map.put(field, resultSet.getLong(fieldsExtensionEnum.tableField.name()));
                        break;
                    }
                    case integer: {
                        map.put(field, resultSet.getInt(fieldsExtensionEnum.tableField.name()));
                        break;
                    }
                    case string: {
                        map.put(field, resultSet.getString(fieldsExtensionEnum.tableField.name()));
                        break;
                    }
                }
            }
            boolean invalidRow = false;
            for (AqlField field : map.keySet()) {
                Object value = map.get(field);
                AqlFieldExtensionEnum fieldExtension = AqlFieldExtensionEnum.getExtensionFor(field);
                if (value == null && !fieldExtension.isNullable()) {
                    invalidRow = true;
                    break;
                }
            }
            if (!invalidRow) {
                rows.add(map);
            }
        }
    }

    @Override
    public int getSize() {
        return rows.size();
    }

    /**
     * @return True if the result set is empty
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public T getResult(int i) {
        return (T) new AqlBaseFullRowImpl(rows.get(i));
    }

    @Override
    public List<T> getResults() {
        List result = Lists.newArrayList();
        for (Map<AqlField, Object> row : rows) {
            result.add(new AqlBaseFullRowImpl(row));
        }
        return result;
    }
}