package org.artifactory.aql.result;

import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.DomainSensitiveField;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * @author Gidi Shabat
 */
public interface AqlLazyResult {

    List<DomainSensitiveField> getFields();

    ResultSet getResultSet();

    int getLimit();

    Map<AqlFieldEnum, String> getDbFieldNames();
}
