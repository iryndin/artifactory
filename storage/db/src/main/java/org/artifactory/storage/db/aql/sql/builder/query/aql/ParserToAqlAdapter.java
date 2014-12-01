package org.artifactory.storage.db.aql.sql.builder.query.aql;

import com.google.common.collect.Lists;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.FieldResolver;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlDomainValueTypeEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlOperatorEnum;
import org.artifactory.aql.model.AqlSortTypeEnum;
import org.artifactory.aql.model.Field;
import org.artifactory.aql.model.Value;
import org.artifactory.aql.model.Variable;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.initable.*;
import org.artifactory.storage.db.aql.sql.builder.query.sql.AqlToSqlQueryBuilderException;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;
import org.artifactory.util.Pair;

import java.util.Iterator;
import java.util.List;

import static org.artifactory.aql.model.AqlField.propertyKey;
import static org.artifactory.aql.model.AqlField.propertyValue;
import static org.artifactory.storage.db.aql.sql.builder.query.sql.type.AqlTableGraph.tablesLinksMap;

/**
 * Converts the parser results into AqlQuery
 *
 * @author Gidi Shabat
 */
public class ParserToAqlAdapter extends AqlAdapter {
    private final OpenParenthesisAqlElement open = new OpenParenthesisAqlElement();
    private final CloseParenthesisAqlElement close = new CloseParenthesisAqlElement();
    private final OperatorQueryElement and = new OperatorQueryElement(AqlOperatorEnum.and);
    private final OperatorQueryElement or = new OperatorQueryElement(AqlOperatorEnum.or);

    /**
     * Converts the parser results into AqlQuery
     * @param parserResult
     * @return
     * @throws AqlException
     */
    public AqlQuery toAqlModel(ParserElementResultContainer parserResult) throws AqlException {
        // Initialize the context
        ParserToAqlAdapterContext context = new ParserToAqlAdapterContext(parserResult.getAll());
        // Set the default operator that is being used if no other operator has been declared.
        context.push(and);
        // Visit the parser result elements and transform them into AqlQuery Object.
        visitElements(context);
        // Finally the AqlQuery is ready;
        return context.getAqlQuery();
    }

    /**
     * Recursively visit the AqlApi parser elements and transform them into AqlQuery Object.
     */
    private void visitElements(ParserToAqlAdapterContext context) throws AqlException {
        if (context.getIndex() < 0) {
            return;
        }
        Pair<ParserElement, String> element = context.getElement();
        if (element.getFirst() instanceof DomainElement) {
            handleDomainFields(context, element);
        }
        if (element.getFirst() instanceof DomainValueTypeElement) {
            handleDomainValueTypes(context);
        }
        if (element.getFirst() instanceof SortTypeElement) {
            handleSort(context);
        }
        if (element.getFirst() instanceof CriteriaEqualsElement) {
            handleCriteriaEquals(context);
        }
        if (element.getFirst() instanceof CriteriaMatchElement) {
            handleCriteriaMatch(context);
        }
        if (element.getFirst() instanceof CriteriaEqualsPropertyElement) {
            handleCriteriaEqualsProperty(context);
        }
        if (element.getFirst() instanceof CriteriaMatchPropertyElement) {
            handleCriteriaMatchProperty(context);
        }
        if (element.getFirst() instanceof FunctionElement) {
            handleFunction(context);
        }
        if (element.getFirst() instanceof CloseParenthesisElement) {
            handleCloseParenthesis(context);
        }
        if (element.getFirst() instanceof LimitValueElement) {
            handleLimit(context);
        }
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Visit recursively next parser elements
        visitElements(context);
    }

    private void handleLimit(ParserToAqlAdapterContext context) {
        // Get the limit value from the element and set it in the context (AqlQuery)
        Pair<ParserElement, String> element = context.getElement();
        int limit = Double.valueOf(element.getSecond()).intValue();
        context.setLimit(limit);
    }

    private void handleCloseParenthesis(ParserToAqlAdapterContext context) {
        // pop operator from operator queue
        context.pop();
        // Push close parenthesis element to context (AqlQuery)
        context.addAqlQueryElements(close);
    }

    private void handleFunction(ParserToAqlAdapterContext context) {
        Pair<ParserElement, String> element = context.getElement();
        AqlOperatorEnum function = AqlOperatorEnum.value(element.getSecond());
        // Add leading operator if needed
        addOperatorToAqlQueryElements(context);
        if (AqlOperatorEnum.freezeJoin == function) {
            // In case of freeze join function generate new alias index for the properties tables
            // All the criterias that uses property table inside the function will use the same table.
            // Push freezeJoin to the operators queue
            context.push(new JoinAqlElement(context.provideIndex()));
        } else if (AqlOperatorEnum.and == function) {
            // Push or and the operators queue
            context.push(and);
        } else if (AqlOperatorEnum.or == function) {
            // Push or to the operators queue
            context.push(or);
        }
        // Push open parenthesis element to context (AqlQuery)
        context.addAqlQueryElements(open);
    }


    private void handleCriteriaEquals(ParserToAqlAdapterContext context)throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria first variable
        String name1 = context.getElement().getSecond();
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // Create equals criteria
        Criteria criteria = createSimpleCriteria(name1, name2, AqlComparatorEnum.equals, context);
        addCriteria(context, criteria);
    }

    private void handleCriteriaMatch(ParserToAqlAdapterContext context)throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria first variable
        String name1 = context.getElement().getSecond();
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria comparator
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(context.getElement().getSecond());
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // Create criteria
        Criteria criteria = createSimpleCriteria(name1, name2, comparatorEnum, context);
        addCriteria(context, criteria);
    }

    private void handleCriteriaMatchProperty(ParserToAqlAdapterContext context)
            throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria first variable
        String name1 = context.getElement().getSecond();
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria comparator
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(context.getElement().getSecond());
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // TODO [By Gidi] I'm not sure if the following if else code should be here or should it be part part of parser
        // Create criteria
        if ("*".equals(name1)) {
            Criteria criteria = createSimpleCriteria(propertyValue.signature, name2, comparatorEnum, context);
            addCriteria(context, criteria);
        } else if ("*".equals(name2)) {
            Criteria criteria = createSimpleCriteria(propertyKey.signature, name1, comparatorEnum, context);
            addCriteria(context, criteria);
        } else {
            Criteria criteria = createPropertyCriteria(name1, name2, comparatorEnum, context);
            addCriteria(context, criteria);
        }
    }

    private void handleCriteriaEqualsProperty(ParserToAqlAdapterContext context)
            throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria first variable
        String name1 = context.getElement().getSecond();
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // Create equals criteria
        Criteria criteria = createPropertyCriteria(name1, name2, AqlComparatorEnum.equals, context);
        addCriteria(context, criteria);
    }

    private void handleSort(ParserToAqlAdapterContext context) throws AqlToSqlQueryBuilderException {
        // Resolve the sortType from the element
        Pair<ParserElement, String> element = context.getElement();
        AqlSortTypeEnum sortTypeEnum = AqlSortTypeEnum.fromAql(element.getSecond());
        SortDetails sortDetails = new SortDetails();
        // Remove two elements from parser result elements
        context.decrementIndex(2);
        Pair<ParserElement, String> currentElement = context.getElement();
        // Get all the sort elements from the following parser elements
        while (currentElement.getFirst() instanceof FieldElement) {
            String fieldName = currentElement.getSecond();
            AqlField field = AqlField.value(fieldName);
            // Ensure that only result fields exist in the sort fields (otherwise sql wil fail)
            if (!context.getResultFields().contains(field)) {
                throw new AqlToSqlQueryBuilderException(
                        "Only the following result fields are allowed to use in the sort section : " + context.getResultFields());
            }
            sortDetails.addField(field);
            // Remove elements from parser result elements
            context.decrementIndex(1);
            currentElement = context.getElement();
        }
        sortDetails.setSortType(sortTypeEnum);
        // Set the sort details in the context (AqlQuery)
        context.setSort(sortDetails);
    }

    private void handleDomainValueTypes(ParserToAqlAdapterContext context) throws AqlException {
        Pair<ParserElement, String> element = context.getElement();
        // Get the domain value type from parser element
        AqlDomainValueTypeEnum valueTypeEnum = AqlDomainValueTypeEnum.value(element.getSecond());
        // Remove two elements from parser result elements
        context.decrementIndex(2);
        // continue handling the parser results elements according to the domainValueTypeEnum value
        switch (valueTypeEnum) {
            case name: {
                handlePropertiesDomain(context, propertyKey);
                break;
            }
            case fields: {
                handleAdditionalFields(context);
                break;
            }
            case values: {
                handlePropertiesDomain(context, propertyValue);
                break;
            }
        }
    }

    private void handleAdditionalFields(ParserToAqlAdapterContext context) throws AqlToSqlQueryBuilderException {
        Pair<ParserElement, String> tempElement = context.getElement();
        // while parser result is not CloseBracketsElement then read next extra field
        // And add it to the result fields
        while (!(tempElement.getFirst() instanceof CloseBracketsElement)) {
            Variable variable = FieldResolver.resolve(tempElement.getSecond());
            if (variable instanceof Field) {
                context.addField(((Field) variable).getFieldEnum());
            } else {
                throw new AqlToSqlQueryBuilderException("Expecting fields inside the Domain fields section");
            }
            // Remove element from parser result elements
            context.decrementIndex(1);
            tempElement = context.getElement();
        }
    }

    private void handlePropertiesDomain(ParserToAqlAdapterContext context, AqlField field)
            throws AqlException {
        List<String> keys = Lists.newArrayList();
        Pair<ParserElement, String> tempElement = context.getElement();
        // while parser result is not CloseBracketsElement then read next value
        // And add new criteria which tables are bind to the default tables
        while (!(tempElement.getFirst() instanceof CloseParenthesisElement)) {
            Variable variable = FieldResolver.resolve(tempElement.getSecond());
            if (variable instanceof Value) {
                keys.add(tempElement.getSecond());
            } else {
                throw new AqlToSqlQueryBuilderException("Expecting values inside the Domain values section");
            }
            context.decrementIndex(1);
            tempElement = context.getElement();
        }
        Iterator<String> iterator = keys.iterator();
        context.addAqlQueryElements(open);
        while (iterator.hasNext()) {
            String next = iterator.next();
            Pair<Variable, Variable> variables = FieldResolver.resolve(field.signature, next);
            SqlTable table = tablesLinksMap.get(SqlTableEnum.node_props).getTable();
            SimpleCriteria simpleCriteria = new SimpleCriteria(variables.getFirst(), table,
                    AqlComparatorEnum.equals.signature, variables.getSecond(), null);
            context.addAqlQueryElements(simpleCriteria);
            if (iterator.hasNext()) {
                context.addAqlQueryElements(or);
            }
        }
        context.addAqlQueryElements(close);
    }

    private void handleDomainFields(ParserToAqlAdapterContext context, Pair<ParserElement, String> element) {
        // resolve the result fields from the domain and add the field to the context (AqlQuery)
        AqlDomainEnum domainEnum = AqlDomainEnum.valueOf(element.getSecond());
        for (AqlField field : domainEnum.fields) {
            context.addField(field);
        }
        context.setDomain(domainEnum);
    }
}