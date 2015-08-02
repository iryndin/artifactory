package org.artifactory.aql.result;

import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.aql.model.DomainSensitiveField;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * @author Gidi Shabat
 */
public interface AqlLazyResult {

    AqlPermissionProvider getPermissionProvider();
    List<DomainSensitiveField> getFields();

    ResultSet getResultSet();

    long getLimit();

    long getOffset();

    Map<AqlFieldEnum, String> getDbFieldNames();

    AqlDomainEnum getDomain();
}
