package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.AqlFieldResolver;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlVariable;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class PropertyCriteria extends Criteria {
    public PropertyCriteria(List<AqlDomainEnum> subDomains, AqlVariable variable1, SqlTable table1,
            String comparatorName,
            AqlVariable variable2, SqlTable table2) {
        super(subDomains, variable1, table1, comparatorName, variable2, table2);
    }

    /**
     * Converts propertyCriteria to Sql criteria
     *
     * @param params
     * @return
     * @throws AqlException
     */
    @Override
    public String toSql(List<Object> params) throws AqlException {
        // Get both variable which are Values (this is property criteria)
        AqlVariable value1 = getVariable1();
        AqlVariable value2 = getVariable2();
        // Get both tables which are node_props tables (this is property criteria)
        SqlTable table1 = getTable1();
        SqlTable table2 = getTable2();
        // update the Sql input param list
        tryToAddParam(params, value1);
        tryToAddParam(params, value2);
        // Get the ComparatorEnum
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(getComparatorName());
        AqlVariable key = AqlFieldResolver.resolve(AqlFieldEnum.propertyKey.signature);
        AqlVariable value = AqlFieldResolver.resolve(AqlFieldEnum.propertyValue.signature);
        boolean sign = AqlComparatorEnum.notEquals == comparatorEnum || AqlComparatorEnum.notMatches == comparatorEnum;
        String relation = sign ? " or " : " and ";
        AqlComparatorEnum equal = sign ? AqlComparatorEnum.notEquals : AqlComparatorEnum.equals;
        if (sign) {
            return "((" + createSqlCriteria(equal, key, table1, table2, value1) + " or" +
                    createSqlCriteria(comparatorEnum, value, table1, table2, value2) + ")" + relation + "(" +
                    a(key, table1, value1, table2, true) + " and " + a(value, table1, value2, table2,
                    true) + "))";
        } else {
            return "(" + createSqlCriteria(equal, key, table1, table2, value1) + " and" +
                    createSqlCriteria(comparatorEnum, value, table1, table2, value2) + ")";
        }
    }
}
