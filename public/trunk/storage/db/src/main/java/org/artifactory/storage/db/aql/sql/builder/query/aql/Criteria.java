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
            AqlVariable variable2,SqlTable table2) {
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
            return " " + table1.getAlias() + toSql(variable1) + nullRelation;
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

    protected Object resolveParam(List<Object> params, AqlVariable variable) throws AqlException {
        AqlValue value = (AqlValue) variable;
        Object param = value.toObject();
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(getComparatorName());
        if (param != null &&
                (AqlComparatorEnum.matches.equals(comparatorEnum) ||
                        AqlComparatorEnum.notMatches.equals(comparatorEnum))) {
            String modifiedValue = (String) param;
            modifiedValue = modifiedValue.replace('*', '%');
            modifiedValue = modifiedValue.replace('?', '_');
            param = modifiedValue;
        }
        return param;
    }

    public String createSqlCriteria(AqlComparatorEnum comparatorEnum, AqlVariable variable1, SqlTable table1,
            AqlVariable variable2, List<Object> params) {
        Object param = resolveParam(params, variable2);
        String index1 = table1 != null && variable1 instanceof AqlField ? table1.getAlias() : "";
        switch (comparatorEnum) {
            case equals: {
                if (param != null) {
                    params.add(param);
                    return " " + index1 + toSql(variable1) + " = " + toSql(variable2);
                } else {
                    return " " + index1 + toSql(variable1) + " is null";
                }
            }
            case matches: {
                validateNotNullParam(param);
                params.add(param);
                if (variable2 instanceof AqlField) {
                    throw new AqlToSqlQueryBuilderException(
                            "Illegal syntax the 'match' operator is allowed only with 'value' in right side of the criteria.");
                }
                return " " + index1 + toSql(variable1) + " like " + toSql(variable2);
            }
            case notMatches: {
                validateNotNullParam(param);
                params.add(param);
                if (variable2 instanceof AqlField) {
                    throw new AqlToSqlQueryBuilderException(
                            "Illegal syntax the 'not match' operator is allowed only with 'value' in right side of the criteria.");
                }
                return "(" + index1 + toSql(variable1) + " not like " + toSql(variable2) + " or " + index1 + toSql(
                        variable1) + " is null)";
            }
            case less: {
                validateNotNullParam(param);
                params.add(param);
                return " " + index1 + toSql(variable1) + " < " + toSql(variable2);
            }
            case greater: {
                validateNotNullParam(param);
                params.add(param);
                return " " + index1 + toSql(variable1) + " > " + toSql(variable2);
            }
            case greaterEquals: {
                validateNotNullParam(param);
                params.add(param);
                return " " + index1 + toSql(variable1) + " >= " + toSql(variable2);
            }
            case lessEquals: {
                validateNotNullParam(param);
                params.add(param);
                return " " + index1 + toSql(variable1) + " <= " + toSql(variable2);
            }
            case notEquals: {
                if (param != null) {
                    params.add(param);
                    return "(" + index1 + toSql(variable1) + " != " + toSql(variable2) + " or " + index1 + toSql(
                            variable1) + " is null)";
                } else {
                    return " " + index1 + toSql(variable1) + " is not null";
                }
            }
            default:
                throw new IllegalStateException("Should not reach to the point of code");
        }
    }

    private void validateNotNullParam(Object param) {
        if (param == null) {
            throw new AqlToSqlQueryBuilderException(
                    "Illegal syntax the 'null' values are allowed to use only with equals and not equals operators.\n");
        }
    }
}
