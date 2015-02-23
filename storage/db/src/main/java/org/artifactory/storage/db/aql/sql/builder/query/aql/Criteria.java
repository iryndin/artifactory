package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlValue;
import org.artifactory.aql.model.AqlVariable;
import org.artifactory.storage.db.aql.sql.builder.query.sql.AqlToSqlQueryBuilderException;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;

import java.util.List;

/**
 * Abstract class that represent single criteria (field comparator and value).
 *
 * @author Gidi Shabat
 */
public abstract class Criteria implements AqlQueryElement {
    private List<AqlDomainEnum> subDomains;
    private AqlVariable variable1;
    private String comparatorName;
    private AqlVariable variable2;
    private SqlTable table1;
    private SqlTable table2;

    public Criteria(List<AqlDomainEnum> subDomains, AqlVariable variable1, SqlTable table1, String comparatorName,
            AqlVariable variable2,
            SqlTable table2) {
        this.subDomains = subDomains;
        this.variable1 = variable1;
        this.table1 = table1;
        this.comparatorName = comparatorName;
        this.variable2 = variable2;
        this.table2 = table2;
    }

    public AqlVariable getVariable1() {
        return variable1;
    }

    public String getComparatorName() {
        return comparatorName;
    }

    public AqlVariable getVariable2() {
        return variable2;
    }

    public SqlTable getTable1() {
        return table1;
    }

    public SqlTable getTable2() {
        return table2;
    }

    public List<AqlDomainEnum> getSubDomains() {
        return subDomains;
    }

    public abstract String toSql(List<Object> params) throws AqlException;

    @Override
    public boolean isOperator() {
        return false;
    }

    protected String a(AqlVariable variable1, SqlTable table1, AqlVariable variable2, SqlTable table2, boolean not) {
        String nullRelation = not ? " is null " : " is not null ";
        if (variable1 instanceof AqlField && variable2 instanceof AqlField) {
            return " (" + table1.getAlias() + toSql(
                    variable1) + nullRelation + " and " + table2.getAlias() + toSql(
                    variable2) + nullRelation + ")";
        }
        if (variable2 instanceof AqlField) {
            return " " + table2.getAlias() + toSql(variable2) + nullRelation;
        }
        if (variable1 instanceof AqlField) {
            return " " + table1.getAlias() + toSql(variable1) + nullRelation;
        }
        return null;
    }

    private String toSql(AqlVariable variable) {
        if (variable instanceof AqlField) {
            AqlFieldEnum fieldEnum = ((AqlField) variable).getFieldEnum();
            AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(fieldEnum);
            return extension.tableField.name();
        } else {
            return "?";
        }
    }

    protected void tryToAddParam(List<Object> params, AqlVariable variable) throws AqlException {
        if (variable instanceof AqlValue) {
            AqlValue value = (AqlValue) variable;
            AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(getComparatorName());
            if (AqlComparatorEnum.matches.equals(comparatorEnum) || AqlComparatorEnum.notMatches.equals(
                    comparatorEnum)) {
                String modifiedValue = (String) value.toObject();
                modifiedValue = modifiedValue.replace('*', '%');
                modifiedValue = modifiedValue.replace('?', '_');
                params.add(modifiedValue);
            } else {
                params.add(value.toObject());
            }
        }
    }

    public String createSqlCriteria(AqlComparatorEnum comparatorEnum, AqlVariable variable1, SqlTable table1,
            SqlTable table2,
            AqlVariable variable2)
            throws AqlToSqlQueryBuilderException {
        String index1 = table1 != null && variable1 instanceof AqlField ? table1.getAlias() : "";
        String index2 = table2 != null && variable2 instanceof AqlField ? table2.getAlias() : "";
        switch (comparatorEnum) {
            case equals: {
                return " " + index1 + toSql(variable1) + " = " + index2 + toSql(variable2);
            }
            case matches: {
                if (variable2 instanceof AqlField) {
                    throw new AqlToSqlQueryBuilderException(
                            "Illegal syntax the 'match' operator is allowed only with 'value' in right side of the criteria.");
                }
                return " " + index1 + toSql(variable1) + " like " + toSql(variable2);
            }
            case notMatches: {
                if (variable2 instanceof AqlField) {
                    throw new AqlToSqlQueryBuilderException(
                            "Illegal syntax the 'not match' operator is allowed only with 'value' in right side of the criteria.");
                }
                return " " + index1 + toSql(variable1) + " not like " + toSql(variable2);
            }
            case less: {
                return " " + index1 + toSql(variable1) + " < " + index2 + toSql(variable2);
            }
            case greater: {
                return " " + index1 + toSql(variable1) + " > " + index2 + toSql(variable2);
            }
            case greaterEquals: {
                return " " + index1 + toSql(variable1) + " >= " + index2 + toSql(variable2);
            }
            case lessEquals: {
                return " " + index1 + toSql(variable1) + " <= " + index2 + toSql(variable2);
            }
            case notEquals: {
                return " " + index1 + toSql(variable1) + " != " + index2 + toSql(variable2);
            }
            default:
                throw new IllegalStateException("Should not reach to the point of code");
        }
    }
}
