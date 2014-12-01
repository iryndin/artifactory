package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.FieldResolver;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.Field;
import org.artifactory.aql.model.Variable;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;
import org.artifactory.util.Pair;

import java.util.List;

import static org.artifactory.storage.db.aql.sql.builder.query.sql.type.AqlTableGraph.tablesLinksMap;

/**
 * The class contains common methods tha are being used by the AqlApiToAqlAdapter and the ParserToAqlAdapter
 * to convert the API or the parser result to AqlQuery
 *
 * @author Gidi Shabat
 */
public abstract class AqlAdapter {

    /**
     * Create Property criteria: A property criteria is actually two criterias on the same property table
     * Example "license" "$matches" "GPL"
     * The above criteria is being converted to the following SQL query
     * node_props.key equals 'license' and node_props.value like 'GPL'
     * @param name1
     * @param name2
     * @param comparatorEnum
     * @param tableReference
     * @return
     * @throws AqlException
     */
    protected Criteria createPropertyCriteria(String name1, String name2, AqlComparatorEnum comparatorEnum,
            AdapterContext tableReference) throws AqlException {
        Pair<Variable, Variable> variables = FieldResolver.resolve(name1, name2);
        Pair<SqlTable, SqlTable> tables = resolveTableForPropertyCriteria(tableReference);
        return new PropertyCriteria(variables.getFirst(), tables.getFirst(),
                comparatorEnum.signature, variables.getSecond(), tables.getSecond());
    }

    /**
     * Create Simple criteria: A simple criteria is actually single criteria constructed as following
     * Field Comparator Value
     * Example the "artifact_repo" "$matches" "libs-release-local"
     * The above criteria is being converted to the following SQL query * node.repo like 'libs-release-local'
     * @param name1
     * @param name2
     * @param comparatorEnum
     * @param context
     * @return
     * @throws AqlException
     */
    protected Criteria createSimpleCriteria(String name1, String name2,
            AqlComparatorEnum comparatorEnum, AdapterContext context)
            throws AqlException {
        Pair<Variable, Variable> variables = FieldResolver.resolve(name1, name2);
        Pair<SqlTable, SqlTable> tables = resolveTableForSimpleCriteria(variables, context);
        return new SimpleCriteria(variables.getFirst(), tables.getFirst(), comparatorEnum.signature,
                variables.getSecond(), tables.getSecond());
    }

    /**
     * Resolving tables is delicate issue.
     * The tables are provided as follow:
     * 1. If the table is not property table then use the default tables from the tablesLinksMap.
     * 2. if the table is property table then by default generate new table with new alias id to each property table
     *    unless the criteria is inside freezeJoin function, and in such case use the table index provided in the
     *    join operator
     *
     * @param variables
     * @param context
     * @return
     */
    public static Pair<SqlTable, SqlTable> resolveTableForSimpleCriteria(Pair<Variable, Variable> variables,
            AdapterContext context) {
        Field field = (Field) variables.getFirst();
        AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(field.getFieldEnum());
        SqlTableEnum tableEnum = extension.table;
        if (SqlTableEnum.node_props == tableEnum) {
            JoinAqlElement propertyAqlElement = (JoinAqlElement) getJoinOperator(context);
            if (propertyAqlElement == null) {
                return new Pair<>(new SqlTable(tableEnum, context.provideIndex()), null);
            } else {
                SqlTable table = new SqlTable(tableEnum, propertyAqlElement.getTableId());
                return new Pair<>(table, table);
            }
        } else {
            return new Pair<>(tablesLinksMap.get(tableEnum).getTable(), null);
        }
    }

    /**
     * Resolving tables is delicate issue, in this case we now that the tables are properties therefore
     * by default generate new table with new alias id to each property table unless the criteria is inside freezeJoin
     * function, and in such case use the table index provided in the
     *    join operator
     *
     * @param context
     * @return
     */
    public static Pair<SqlTable, SqlTable> resolveTableForPropertyCriteria(AdapterContext context) {
        JoinAqlElement propertyAqlElement = (JoinAqlElement) getJoinOperator(context);
        if (propertyAqlElement == null) {
            SqlTable table = new SqlTable(SqlTableEnum.node_props, context.provideIndex());
            return new Pair<>(table, table);
        } else {
            SqlTable table = new SqlTable(SqlTableEnum.node_props, propertyAqlElement.getTableId());
            return new Pair<>(table, table);
        }
    }

    /**
     * Adds criteria to the AqlQuery and its leading operator if needed
     * @param context
     * @param criteria
     */
    protected void addCriteria(AdapterContext context, Criteria criteria) {
        addOperatorToAqlQueryElements(context);
        context.addAqlQueryElements(criteria);
    }

    /**
     * Scans the context que for leading join operators
     * @param context
     * @return
     */
    protected static AqlQueryElement getJoinOperator(AdapterContext context) {
        if (context.getFunctions().isEmpty()) {
            return null;
        }
        AqlQueryElement peek = context.peek();
        if (peek instanceof JoinAqlElement) {
            return peek;
        }
        AqlQueryElement temp = context.pop();
        peek = getJoinOperator(context);
        context.push(temp);
        return peek;
    }

    /**
     * Scans the context que for leading or/and operators
     * @param context
     * @return
     */
    protected static AqlQueryElement getOperator(AdapterContext context) {
        if (context.getFunctions().isEmpty()) {
            return null;
        }
        AqlQueryElement peek = context.peek();
        if (peek.isOperator()) {
            return peek;
        }
        AqlQueryElement temp = context.pop();
        peek = getOperator(context);
        context.push(temp);
        return peek;
    }

    /**
     * Adds operator to the AqlQuery if needed
     * @param context
     */
    protected static void addOperatorToAqlQueryElements(AdapterContext context) {
        List<AqlQueryElement> aqlQueryElements = context.getAqlQueryElements();
        if (!aqlQueryElements.isEmpty() && (aqlQueryElements.get(aqlQueryElements.size() - 1) instanceof Criteria ||
                aqlQueryElements.get(aqlQueryElements.size() - 1) instanceof CloseParenthesisAqlElement)) {
            context.addAqlQueryElements(getOperator(context));
        }
    }
}
