package org.artifactory.storage.db.aql.sql.builder.query.aql;

import com.google.common.collect.Lists;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlOperatorEnum;
import org.artifactory.aql.model.AqlSortTypeEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.basic.language.*;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.CriteriaEqualsElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.CriteriaEqualsPropertyElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.CriteriaMatchElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.CriteriaMatchPropertyElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.DomainElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.FunctionElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive.IncludeExtensionElement;
import org.artifactory.storage.db.aql.sql.builder.query.sql.AqlToSqlQueryBuilderException;
import org.artifactory.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.artifactory.aql.model.AqlFieldEnum.propertyKey;
import static org.artifactory.aql.model.AqlFieldEnum.propertyValue;

/**
 * Converts the parser results into AqlQuery
 *
 * @author Gidi Shabat
 */
public class ParserToAqlAdapter extends AqlAdapter {
    private final FlatAqlElement resultField = new FlatAqlElement();

    /**
     * Converts the parser results into AqlQuery
     *
     * @param parserResult
     * @return
     * @throws AqlException
     */
    public AqlQuery toAqlModel(ParserElementResultContainer parserResult) throws AqlException {
        // Initialize the context
        ParserToAqlAdapterContext context = new ParserToAqlAdapterContext(parserResult.getAll());
        // Set the default operator that is being used if no other operator has been declared.
        context.push(and);
        // Resolve domain inf.
        handleDomainFields(context);
        // Resolve include fields info.
        handleIncludeFields(context);
        // Resolve sort info.
        handleSort(context);
        // Resolve limit info.
        handleLimit(context);
        // Resolve Filter info
        handleFilter(context);
        // Add default filters
        injectDefaultValues(context);
        // Finally the AqlQuery is ready;
        return context.getAqlQuery();
    }

    /**
     * Converts the Criterias from the parser results into Aql criterias
     *
     * @param context
     */
    private void handleFilter(ParserToAqlAdapterContext context) {
        context.resetIndex();
        while (context.hasNext()) {
            Pair<ParserElement, String> element = context.getElement();
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
            if (element.getFirst() instanceof OpenParenthesisElement) {
                handleOpenParenthesis(context);
            }
            if (element.getFirst() instanceof SectionEndElement ||
                    element.getFirst() instanceof IncludeTypeElement) {
                return;
            }
            // Promote element
            context.decrementIndex(1);
        }
    }

    private void handleOpenParenthesis(ParserToAqlAdapterContext context) {
        // Add parenthesis element to the AqlQuery
        context.addAqlQueryElements(open);
    }

    private void handleIncludeFields(ParserToAqlAdapterContext context) {
        // Initialize the context
        gotoElement(IncludeExtensionElement.class, context);
        if (!context.hasNext()) {
            return;
        }
        // Scan all the include domain anf fields
        context.decrementIndex(1);
        boolean first = false;
        // Prepare the context for include property filter do not worry about empty parenthesis because the AqlOptimizer will clean it
        context.push(or);
        context.addAqlQueryElements(open);
        while (!(context.getElement().getFirst() instanceof SectionEndElement)) {
            // Resolve the field sub domains
            List<AqlDomainEnum> subDomains = resolveSubDomains(context);
            if (context.getElement().getFirst() instanceof RealFieldElement) {
                // Extra field
                first = handleIncludeExtraField(context, first, subDomains);
            } else if (context.getElement().getFirst() instanceof IncludeDomainElement ||
                    context.getElement().getFirst() instanceof EmptyIncludeDomainElement) {
                // Extra domain
                handleIncludeDomain(context, subDomains);
            } else {
                //Extra property result filter
                handleIncludePropertyKeyFilter(context, subDomains);
            }
        }
        context.addAqlQueryElements(close);
        context.pop();
    }

    /**
     * If the extra field belongs to the main query domain then remove all the default fields and add only the fields from
     * the include section.
     * If the extra field doesn't belongs to the main domain then just add the field to the result fields.
     *
     * @param context
     * @param overrideFields
     * @param subDomains
     * @return
     */
    private boolean handleIncludeExtraField(ParserToAqlAdapterContext context, boolean overrideFields,
            List<AqlDomainEnum> subDomains) {
        // If the extra field belongs to the main domain then remove all the default fields ()
        if (!overrideFields && subDomains.size() == 1) {
            AqlDomainEnum mainDomain = subDomains.get(0);
            List<DomainSensitiveField> resultFields = context.getResultFields();
            Iterator<DomainSensitiveField> iterator = resultFields.iterator();
            while (iterator.hasNext()) {
                DomainSensitiveField next = iterator.next();
                AqlDomainEnum aqlDomainEnum = AqlDomainEnum.valueOf(next.getField().domainName);
                if (aqlDomainEnum.equals(mainDomain)) {
                    iterator.remove();
                }
            }
            overrideFields = true;
        }
        AqlFieldEnum aqlField = resolveField(context);
        DomainSensitiveField field = new DomainSensitiveField(aqlField, subDomains);
        context.addField(field);
        context.decrementIndex(1);
        return overrideFields;
    }

    /**
     * Special case for properties that allows to  add property key to return specific property
     *
     * @param context
     * @param subDomains
     */
    private void handleIncludePropertyKeyFilter(ParserToAqlAdapterContext context, List<AqlDomainEnum> subDomains) {
        context.addField(new DomainSensitiveField(AqlFieldEnum.propertyKey, subDomains));
        context.addField(new DomainSensitiveField(AqlFieldEnum.propertyValue, subDomains));
        String value = context.getElement().getSecond();
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.matches;
        // Only if the user has specify property key to filter then add the filter else just add the fields.
        if (!"*".equals(value)) {
            context.push(resultField);
            Criteria criteria = createSimpleCriteria(subDomains, AqlFieldEnum.propertyKey, value, comparatorEnum,
                    context);
            addCriteria(context, criteria);
            context.pop();
        }
        context.decrementIndex(1);
    }

    /**
     * Allows to add the domain fields to the result fields
     *
     * @param context
     * @param subDomains
     */
    private void handleIncludeDomain(ParserToAqlAdapterContext context, List<AqlDomainEnum> subDomains) {
        DomainProviderElement element = (DomainProviderElement) context.getElement().getFirst();
        AqlDomainEnum aqlDomainEnum = element.getDomain();
        AqlFieldEnum[] fieldByDomain = AqlFieldEnum.getFieldByDomain(aqlDomainEnum);
        for (AqlFieldEnum aqlFieldEnum : fieldByDomain) {
            context.addField(new DomainSensitiveField(aqlFieldEnum, subDomains));
        }
        context.decrementIndex(1);
    }

    /**
     * Allows to add limit to the query in order to limit the number of rows returned
     * @param context
     */
    private void handleLimit(ParserToAqlAdapterContext context) {
        gotoElement(LimitValueElement.class, context);
        if (!context.hasNext()) {
            return;
        }
        // Get the limit value from the element and set it in the context (AqlQuery)
        Pair<ParserElement, String> element = context.getElement();
        int limit = Double.valueOf(element.getSecond()).intValue();
        context.setLimit(limit);
    }

    private void handleCloseParenthesis(ParserToAqlAdapterContext context) {
        // Pop operator from operator queue
        context.pop();
        // Push close parenthesis element to context (AqlQuery)
        context.addAqlQueryElements(close);
    }

    /**
     * Handles operator "and"/"or" operators and the "freezJon"/"resultFields" functions
     * @param context
     */
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
        } else if (AqlOperatorEnum.resultFilter == function) {
            // Push or to the operators queue
            context.push(resultField);
        }
    }


    private void handleCriteriaEquals(ParserToAqlAdapterContext context) throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the field sub domains
        List<AqlDomainEnum> subDomain = resolveSubDomains(context);
        // Get the criteria first variable
        AqlFieldEnum aqlField = resolveField(context);
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // Create equals criteria
        Criteria criteria = createSimpleCriteria(subDomain, aqlField, name2, AqlComparatorEnum.equals, context);
        addCriteria(context, criteria);
    }

    private void handleCriteriaMatch(ParserToAqlAdapterContext context) throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the field sub domains
        List<AqlDomainEnum> subDomains = resolveSubDomains(context);
        // Get the criteria first field
        AqlFieldEnum aqlField = resolveField(context);
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria comparator
        AqlComparatorEnum comparatorEnum = AqlComparatorEnum.value(context.getElement().getSecond());
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // Create criteria
        Criteria criteria = createSimpleCriteria(subDomains, aqlField, name2, comparatorEnum, context);
        addCriteria(context, criteria);
    }

    private void handleCriteriaMatchProperty(ParserToAqlAdapterContext context)
            throws AqlException {
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the field sub domains
        List<AqlDomainEnum> subDomains = resolveSubDomains(context);
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
        // TODO [By Gidi] I'm not sure if the following if else code should be here or should it be part part of parser or part of the optimizer
        // Create criteria
        if ("*".equals(name1)) {
            Criteria criteria = createSimpleCriteria(subDomains, propertyValue, name2, comparatorEnum, context);
            addCriteria(context, criteria);
        } else if ("*".equals(name2)) {
            Criteria criteria = createSimpleCriteria(subDomains, propertyKey, name1, comparatorEnum, context);
            addCriteria(context, criteria);
        } else {
            Criteria criteria = createPropertyCriteria(subDomains, name1, name2, comparatorEnum, context);
            addCriteria(context, criteria);
        }
    }

    private void handleCriteriaEqualsProperty(ParserToAqlAdapterContext context)
            throws AqlException {

        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the field sub domains
        List<AqlDomainEnum> subDomains = resolveSubDomains(context);
        // Get the criteria first variable
        String name1 = context.getElement().getSecond();
        // Remove element from parser result elements
        context.decrementIndex(1);
        // Get the criteria second variable
        String name2 = context.getElement().getSecond();
        // Create equals criteria
        if ("*".equals(name1)) {
            Criteria criteria = createSimpleCriteria(subDomains, propertyValue, name2, AqlComparatorEnum.equals,
                    context);
            addCriteria(context, criteria);
        } else if ("*".equals(name2)) {
            Criteria criteria = createSimpleCriteria(subDomains, propertyKey, name1, AqlComparatorEnum.equals, context);
            addCriteria(context, criteria);
        } else {
            Criteria criteria = createPropertyCriteria(subDomains, name1, name2, AqlComparatorEnum.equals, context);
            addCriteria(context, criteria);
        }

    }

    private void handleSort(ParserToAqlAdapterContext context) throws AqlToSqlQueryBuilderException {
        gotoElement(SortTypeElement.class, context);
        if (!context.hasNext()) {
            return;
        }
        // Resolve the sortType from the element
        Pair<ParserElement, String> element = context.getElement();
        AqlSortTypeEnum sortTypeEnum = AqlSortTypeEnum.fromAql(element.getSecond());
        SortDetails sortDetails = new SortDetails();
        // Remove two elements from parser result elements
        context.decrementIndex(2);
        Pair<ParserElement, String> currentElement = context.getElement();
        // Get all the sort elements from the following parser elements
        while (!(currentElement.getFirst() instanceof CloseParenthesisElement)) {
            // Get the field sub domains
            List<AqlDomainEnum> subDomain = resolveSubDomains(context);
            AqlFieldEnum field = resolveField(context);
            // Remove element from parser result elements
            context.decrementIndex(1);
            DomainSensitiveField domainSensitiveField = new DomainSensitiveField(field, subDomain);
            if (!context.getResultFields().contains(domainSensitiveField)) {
                throw new AqlToSqlQueryBuilderException(
                        "Only the result fields are allowed to use in the sort section.");
            }
            sortDetails.addField(field);
            // Get the current element;
            currentElement = context.getElement();
        }
        sortDetails.setSortType(sortTypeEnum);
        // Set the sort details in the context (AqlQuery)
        context.setSort(sortDetails);
    }

    private List<AqlDomainEnum> resolveSubDomains(ParserToAqlAdapterContext context) {
        Pair<ParserElement, String> element = context.getElement();
        List<AqlDomainEnum> list = Lists.newArrayList();
        while (!(element.getFirst() instanceof RealFieldElement || element.getFirst() instanceof ValueElement ||
                element.getFirst() instanceof IncludeDomainElement
                || element.getFirst() instanceof EmptyIncludeDomainElement)) {
            list.add(((DomainProviderElement) element.getFirst()).getDomain());
            context.decrementIndex(1);
            element = context.getElement();
        }
        if (element.getFirst() instanceof EmptyIncludeDomainElement) {
            list.add(((DomainProviderElement) element.getFirst()).getDomain());
        }
        return list;
    }

    private AqlFieldEnum resolveField(ParserToAqlAdapterContext context) {
        Pair<ParserElement, String> element = context.getElement();
        String fieldName = element.getSecond();
        AqlDomainEnum domain = ((DomainProviderElement) context.getElement().getFirst()).getDomain();
        return AqlFieldEnum.resolveFieldBySignatureAndDomain(fieldName, domain);
    }


    private void handleDomainFields(ParserToAqlAdapterContext context) {
        // resolve the result fields from the domain and add the field to the context (AqlQuery)
        gotoElement(DomainElement.class, context);
        context.decrementIndex(1);
        Pair<ParserElement, String> element = context.getElement();
        ArrayList<String> subdomains = Lists.newArrayList();
        while (element.getFirst() instanceof DomainSubPathElement) {
            subdomains.add(element.getSecond());
            context.decrementIndex(1);
            element = context.getElement();
        }
        AqlDomainEnum domain = AqlDomainEnum.valueFromSubDomains(subdomains);
        context.setDomain(domain);
        for (AqlFieldEnum field : domain.fields) {
            context.addField(new DomainSensitiveField(field, Lists.newArrayList(domain)));
        }
    }

    private void gotoElement(Class<? extends ParserElement> domainElementClass, ParserToAqlAdapterContext context) {
        context.resetIndex();
        while (context.hasNext() &&
                (!context.getElement().getFirst().getClass().equals(domainElementClass))) {
            context.decrementIndex(1);
        }
    }
}