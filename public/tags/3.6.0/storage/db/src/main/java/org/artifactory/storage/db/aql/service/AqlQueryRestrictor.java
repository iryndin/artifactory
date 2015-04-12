package org.artifactory.storage.db.aql.service;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQueryElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.Criteria;
import org.artifactory.storage.db.aql.sql.builder.query.aql.SortDetails;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class AqlQueryRestrictor {

    public void restrict(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        // Block property result filter for OSS
        blockPropertyResultFilterForOSS(aqlQuery, permissionProvider);
        // Block sorting for OSS
        blockSortingForOSS(aqlQuery, permissionProvider);
    }

    private void blockSortingForOSS(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        if (permissionProvider.isOss()) {
            SortDetails sort = aqlQuery.getSort();
            if (sort != null && sort.getFields() != null && sort.getFields().size() > 0) {
                throw new AqlException("Sorting is not supported by AQL in the open source version\n");
            }
        }
    }

    private void blockPropertyResultFilterForOSS(AqlQuery aqlQuery, AqlPermissionProvider permissionProvider) {
        if (permissionProvider.isOss()) {
            List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
            for (AqlQueryElement aqlElement : aqlElements) {
                if (aqlElement instanceof Criteria) {
                    SqlTable table1 = ((Criteria) aqlElement).getTable1();
                    if (SqlTableEnum.node_props == table1.getTable() && table1.getId() < SqlTable.MINIMAL_DYNAMIC_TABLE_ID) {
                        throw new AqlException(
                                "Filtering properties result is not supported by AQL in the open source version\n");
                    }
                }
            }
        }
    }
}
