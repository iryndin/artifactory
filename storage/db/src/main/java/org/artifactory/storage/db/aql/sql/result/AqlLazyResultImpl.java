package org.artifactory.storage.db.aql.sql.result;

import com.google.common.collect.Maps;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.aql.result.AqlLazyResult;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlQuery;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * @author Gidi Shabat
 */
public class AqlLazyResultImpl implements AqlLazyResult {
    private final int limit;
    private final List<DomainSensitiveField> fields;
    private ResultSet resultSet;
    private Map<AqlFieldEnum, String> dbFieldNames;

    public AqlLazyResultImpl(ResultSet resultSet, SqlQuery sqlQuery) {
        limit = sqlQuery.getLimit();
        fields = sqlQuery.getResultFields();
        this.resultSet = resultSet;
        dbFieldNames = Maps.newHashMap();
        for (DomainSensitiveField field : fields) {
            AqlFieldEnum fieldEnum = field.getField();
            dbFieldNames.put(fieldEnum, AqlFieldExtensionEnum.getExtensionFor(fieldEnum).tableField.name());
        }
    }

    @Override
    public List<DomainSensitiveField> getFields() {
        return fields;
    }

    @Override
    public ResultSet getResultSet() {
        return resultSet;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public Map<AqlFieldEnum, String> getDbFieldNames() {
        return dbFieldNames;
    }
}
