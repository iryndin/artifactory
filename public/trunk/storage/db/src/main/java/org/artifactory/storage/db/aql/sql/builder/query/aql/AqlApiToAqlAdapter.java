package org.artifactory.storage.db.aql.sql.builder.query.aql;

import org.artifactory.aql.FieldResolver;
import org.artifactory.aql.api.Aql;
import org.artifactory.aql.api.AqlApi;
import org.artifactory.aql.api.AqlApiElement;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlOperatorEnum;
import org.artifactory.aql.model.Field;
import org.artifactory.aql.model.Value;
import org.artifactory.aql.model.Variable;
import org.artifactory.storage.db.aql.sql.builder.links.TableLink;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;
import org.artifactory.util.Pair;

import java.util.List;
import java.util.Map;

import static org.artifactory.storage.db.aql.sql.builder.query.sql.type.AqlTableGraph.tablesLinksMap;

/**
 * Transform the AqlApi element into AqlQuery.
 *
 * @author Gidi Shabat
 */
public class AqlApiToAqlAdapter extends AqlAdapter {

    private final OpenParenthesisAqlElement open = new OpenParenthesisAqlElement();
    private final CloseParenthesisAqlElement close = new CloseParenthesisAqlElement();
    private final OperatorQueryElement and = new OperatorQueryElement(AqlOperatorEnum.and);
    private final OperatorQueryElement or = new OperatorQueryElement(AqlOperatorEnum.or);

    /**
     * Converts Aql (Api) into SqlQuery
     *
     * @param aqlApi
     * @return
     */
    public AqlQuery toAqlModel(Aql aqlApi) {
        // Initialize the context
        AdapterContext context = new AdapterContext();
        // Set the default operator that is being used if no other operator has been declared.
        context.push(and);
        // Recursively visit the AqlApi elements anf fill the AqlQuery
        visitElements(aqlApi, context);
        return context.getAqlQuery();
    }

    /**
     * Recursively visit the AqlApi parser elements and transform them into AqlQuery Object.
     */
    private void visitElements(AqlApiElement rootElement, AdapterContext context) {
        List<AqlApiElement> list = rootElement.get();
        for (AqlApiElement element : list) {
            if (element instanceof AqlApi.SortApiElement) {
                handleSort((AqlApi.SortApiElement) element, context);
            }
            if (element instanceof AqlApi.DomainApiElement) {
                handleDomain((AqlApi.DomainApiElement) element, context);
            }
            if (element instanceof AqlApi.AndClause) {
                handleAnd((AqlApi.AndClause) element, context);
            }
            if (element instanceof AqlApi.OrClause) {
                handleOr((AqlApi.OrClause) element, context);
            }
            if (element instanceof AqlApi.FreezeJoin) {
                handleFreezeJoin((AqlApi.FreezeJoin) element, context);
            }
            if (element instanceof AqlApi.PropertyCriteriaClause) {
                handlePropertyCriteria((AqlApi.PropertyCriteriaClause) element, context);
            }
            if (element instanceof AqlApi.CriteriaClause) {
                handleCriteria((AqlApi.CriteriaClause) element, context);
            }
            if (element instanceof AqlApi.LimitApiElement) {
                handleLimit((AqlApi.LimitApiElement) element, context);
            }
            if (element instanceof AqlApi.FilterApiElement) {
                handleFilter((AqlApi.FilterApiElement) element, context);
                visitElements(element, context);
            }
        }
    }

    private void handleLimit(AqlApi.LimitApiElement element, AdapterContext context) {
        //Read the limit value from the AqlApi LimitApiElement and put it in the context (AqlQuery)
        context.setLimit(element.getLimit());
    }

    private void handleFilter(AqlApi.FilterApiElement element, AdapterContext context) {
        // The AqlApi FilterApiElement contains the "RESULT" filter which is being used to filter specific properties
        // and not all the Artifact properties
        // For each entry in the map create criteria binding the criteria to the result field tables
        Map<AqlField, Object> filterMap = element.getResultFilter();
        for (AqlField fieldEnum : filterMap.keySet()) {
            Variable variable1 = FieldResolver.resolve(fieldEnum.signature);
            Variable variable2 = FieldResolver.resolve(filterMap.get(fieldEnum).toString());
            if (variable1 instanceof Field && variable2 instanceof Value) {
                AqlField field = ((Field) variable1).getFieldEnum();
                AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(field);
                TableLink tableLink = tablesLinksMap.get(extension.table);
                Pair<SqlTable, SqlTable> tables = new Pair<>(tableLink.getTable(), null);
                Criteria criteria = new SimpleCriteria(variable1, tables.getFirst(), AqlComparatorEnum.equals.signature,
                        variable2, tables.getSecond());
                addCriteria(context, criteria);
            }
        }
    }

    private void handlePropertyCriteria(AqlApi.PropertyCriteriaClause element, AdapterContext context) {
        // Converts AqlApi propertyCriteriaClause into real PropertyCriteria
        Pair<Variable, Variable> pair = FieldResolver.resolve(element.getString1(), element.getString2());
        Variable variable1 = pair.getFirst();
        Variable variable2 = pair.getSecond();
        Pair<SqlTable, SqlTable> tables = resolveTableForPropertyCriteria(context);
        Criteria criteria = new PropertyCriteria(variable1, tables.getFirst(), element.getComparator().signature,
                variable2, tables.getSecond());
        addCriteria(context, criteria);
    }

    private void handleCriteria(AqlApi.CriteriaClause element, AdapterContext context) {
        // Converts AqlApi CriteriaClause into real criteria
        Pair<Variable, Variable> pair = FieldResolver.resolve(element.getString1(), element.getString2());
        Variable criteriaField = pair.getFirst();
        Variable criteriaValue = pair.getSecond();
        Pair<SqlTable, SqlTable> tables = resolveTableForSimpleCriteria(
                new Pair<>(criteriaField, criteriaValue), context);
        Criteria criteria = new SimpleCriteria(criteriaField, tables.getFirst(), element.getComparator().signature,
                criteriaValue, tables.getSecond());
        addCriteria(context, criteria);
    }

    private void handleFreezeJoin(AqlApi.FreezeJoin freezeJoin, AdapterContext context) {
        if (freezeJoin.isEmpty()) {
            return;
        }
        // Add operator if needed
        addOperatorToAqlQueryElements(context);
        // Push Join element that contains table index, this index will be used by in all the property tables that are
        // being used inside this function
        context.push(new JoinAqlElement(context.provideIndex()));
        context.addAqlQueryElements(open);
        // Recursively visit the internal elements
        visitElements(freezeJoin, context);
        context.addAqlQueryElements(close);
        // Pop the JoinAqlElement, we are getting out from the function
        context.pop();
    }

    private void handleOr(AqlApi.OrClause or, AdapterContext context) {
        if (or.isEmpty()) {
            return;
        }
        // Add operator if needed
        addOperatorToAqlQueryElements(context);
        // Push the or operator
        context.push(this.or);
        context.addAqlQueryElements(open);
        // Recursively visit the internal elements
        visitElements(or, context);
        context.addAqlQueryElements(close);
        // Pop the OrlElement, we are getting out from the function
        context.pop();
    }

    private void handleAnd(AqlApi.AndClause and, AdapterContext context) {
        if (and.isEmpty()) {
            return;
        }
        // Add operator if needed
        addOperatorToAqlQueryElements(context);
        // Push the and operator
        context.push(this.and);
        context.addAqlQueryElements(open);
        // Recursively visit the internal elements
        visitElements(and, context);
        context.addAqlQueryElements(close);
        // Pop the AndlElement, we are getting out from the function
        context.pop();
    }

    private void handleSort(AqlApi.SortApiElement sort, AdapterContext context) {
        // Get the Sort info from the AqlApi SortApiElement and set the info inside the new SortDetails object
        SortDetails sortDetails = new SortDetails();
        if (sort != null && !sort.isEmpty()) {
            sortDetails.setSortType(sort.getSortType());
            for (AqlField aqlField : sort.getFields()) {
                sortDetails.addField(aqlField);
            }
        }
        context.setSort(sortDetails);
    }

    private void handleDomain(AqlApi.DomainApiElement domain, AdapterContext context) {
        // Get the Domain info from the AqlApi DomainApiElement and set in the AqlQuery
        for (AqlDomainEnum queryType : domain.getDomains()) {
            for (AqlField field : queryType.fields) {
                context.addField(field);
            }
        }
        // Get the Domain extra fields info from the AqlApi DomainApiElement and set it in the AqlQuery
        for (AqlField fieldsExtentionEnum : domain.getExtraFields()) {
            context.addField(fieldsExtentionEnum);
        }
        // Get the Main domain info from the AqlApi DomainApiElement and set it in the AqlQuery
        context.setDomain(domain.getDomains()[0]);
    }
}
