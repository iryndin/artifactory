package org.artifactory.storage.db.aql.sql.builder.query.sql.type;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlOperatorEnum;
import org.artifactory.aql.model.AqlTableFieldsEnum;
import org.artifactory.storage.db.aql.sql.builder.links.TableLink;
import org.artifactory.storage.db.aql.sql.builder.links.TableLinkBrowser;
import org.artifactory.storage.db.aql.sql.builder.links.TableLinkRelation;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQueryElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.CloseParenthesisAqlElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.Criteria;
import org.artifactory.storage.db.aql.sql.builder.query.aql.OpenParenthesisAqlElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.OperatorQueryElement;
import org.artifactory.storage.db.aql.sql.builder.query.aql.PropertyCriteria;
import org.artifactory.storage.db.aql.sql.builder.query.aql.SimpleCriteria;
import org.artifactory.storage.db.aql.sql.builder.query.aql.SortDetails;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.model.AqlFieldExtensionEnum;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;
import org.artifactory.util.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.artifactory.aql.model.AqlTableFieldsEnum.node_id;
import static org.artifactory.storage.db.aql.sql.builder.query.sql.type.AqlTableGraph.tablesLinksMap;
import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.nodes;

/**
 * This is actually the class that contains all the code that converts the AqlQuery to sqlQuery.
 *
 * @author Gidi Shabat
 */
public abstract class BasicSqlGenerator {
    public final Map<SqlTableEnum, Map<SqlTableEnum, List<TableLinkRelation>>> tableRouteMap;

    /**
     * The constructor scans the table schema and creates a map that contains the shortest route between to tables
     */
    protected BasicSqlGenerator() {
        Map<SqlTableEnum, Map<SqlTableEnum, List<TableLinkRelation>>> routeMap = Maps.newHashMap();
        for (TableLink from : tablesLinksMap.values()) {
            for (TableLink to : tablesLinksMap.values()) {
                List<TableLinkRelation> route = findShortestPathBetween(from, to);
                Map<SqlTableEnum, List<TableLinkRelation>> toRouteMap = routeMap.get(from.getTableEnum());
                if (toRouteMap == null) {
                    toRouteMap = Maps.newHashMap();
                    routeMap.put(from.getTableEnum(), toRouteMap);
                }
                toRouteMap.put(to.getTableEnum(), route);
            }
        }
        tableRouteMap = routeMap;
    }

    /**
     * The method generates the result part of the SQL query
     */
    public String results(AqlQuery aqlQuery) {
        StringBuilder result = new StringBuilder();
        result.append(" ");
        Iterator<AqlField> iterator = aqlQuery.getResultFields().iterator();
        while (iterator.hasNext()) {
            AqlField nextField = iterator.next();
            AqlFieldExtensionEnum next = AqlFieldExtensionEnum.getExtensionFor(nextField);
            SqlTable table = tablesLinksMap.get(next.table).getTable();
            result.append(table.getAlias()).append(next.tableField);
            if (iterator.hasNext()) {
                result.append(",");
            } else {
                result.append(" ");
            }
        }
        return result.toString();
    }

    public String tables(AqlQuery aqlQuery) {
        Set<SqlTable> usedTables = Sets.newHashSet();
        StringBuilder join = new StringBuilder();
        join.append(" ");
        Iterable<SqlTable> tables1 = Iterables.transform(aqlQuery.getResultFields(), toTables);
        Iterable<AqlQueryElement> filter = Iterables.filter(aqlQuery.getAqlElements(), criteriasOnly);
        Iterable<SqlTable> tables2 = Iterables.transform(filter, firstTableFromCriteria);
        Iterable<SqlTable> tables3 = Iterables.transform(filter, secondTableFromCriteria);
        Iterable<SqlTable> allTables = Iterables.concat(tables1, tables2, tables3);
        AqlJoinTypeEnum joinTypeEnum = resolveJoinType(allTables);
        Iterable<SqlTable> propertiesTables = Iterables.filter(allTables, properties);
        Iterable<SqlTable> nonePropertiesTables = Iterables.filter(allTables, this.noneProperties);
        nonePropertiesTables = Iterables.filter(nonePropertiesTables, notNull);
        SqlTable mainTable = tablesLinksMap.get(getMainTable()).getTable();
        // Join the main table
        joinTable(mainTable, null, null, null, usedTables, join, true, joinTypeEnum);
        // Join the none properties tables
        for (SqlTable table : nonePropertiesTables) {
            TableLink from = tablesLinksMap.get(getMainTable());
            TableLink to = tablesLinksMap.get(table.getTable());
            List<TableLinkRelation> relations = tableRouteMap.get(from.getTableEnum()).get(to.getTableEnum());
            generateJoinTables(relations, usedTables, join, joinTypeEnum);
        }
        // If need to find properties then ensure that the nodes table is joined
        if (propertiesTables.iterator().hasNext()) {
            TableLink from = tablesLinksMap.get(getMainTable());
            TableLink to = tablesLinksMap.get(nodes);
            List<TableLinkRelation> relations = tableRouteMap.get(from.getTableEnum()).get(to.getTableEnum());
            generateJoinTables(relations, usedTables, join, joinTypeEnum);
        }
        // Join properties (provide table for each exist criteria
        for (SqlTable propertiesTable : propertiesTables) {
            SqlTable nodes = tablesLinksMap.get(SqlTableEnum.nodes).getTable();
            joinTable(propertiesTable, node_id, nodes, node_id, usedTables, join, false, joinTypeEnum);
        }
        return join.toString();
    }

    private List<TableLinkRelation> findShortestPathBetween(TableLink from,
            TableLink to) {
        List<TableLinkRelation> relations = TableLinkBrowser.create().findPathTo(from, to, null, getExclude());
        if (relations == null) {
            ArrayList<TableLink> excludes = Lists.newArrayList();
            relations = TableLinkBrowser.create().findPathTo(from, to, null, excludes);
        }
        return relations;
    }

    protected abstract List<TableLink> getExclude();

    protected void generateJoinTables(List<TableLinkRelation> relations, Set<SqlTable> usedTables, StringBuilder join,
            AqlJoinTypeEnum joinTypeEnum) {
        if (relations == null) {
            return;
        }
        for (TableLinkRelation relation : relations) {
            AqlTableFieldsEnum fromField = relation.getFromField();
            SqlTable fromTable = relation.getFromTable().getTable();
            AqlTableFieldsEnum toFiled = relation.getToFiled();
            SqlTable toTable = relation.getToTable().getTable();
            joinTable(toTable, toFiled, fromTable, fromField, usedTables, join, false, joinTypeEnum);
        }
    }

    protected boolean joinTable(SqlTable table, AqlTableFieldsEnum tableJoinField, SqlTable onTable, AqlTableFieldsEnum onJoinFiled,
            Set<SqlTable> declaredTables, StringBuilder join, boolean first, AqlJoinTypeEnum joinTypeEnum) {

        if (!declaredTables.contains(table)) {
            if (first) {
                join.append(table.getTableName()).append(" ").append(table.getAliasDeclaration());
                first = false;
            } else {
                join.append(" ").append(joinTypeEnum.signature).append(" ").append(table.getTableName()).append(" ").append(
                        table.getAliasDeclaration());
                join.append(" on ").append(table.getAlias()).append(tableJoinField).
                        append(" = ").append(onTable.getAlias()).append(onJoinFiled);
            }
        }
        declaredTables.add(table);
        return first;
    }

    public Pair<String, List<Object>> conditions(AqlQuery aqlQuery)
            throws AqlException {
        StringBuilder condition = new StringBuilder();
        List<Object> params = Lists.newArrayList();
        for (AqlQueryElement aqlQueryElement : aqlQuery.getAqlElements()) {
            if (aqlQueryElement instanceof PropertyCriteria || aqlQueryElement instanceof SimpleCriteria) {
                Criteria criteria = (Criteria) aqlQueryElement;
                condition.append(criteria.toSql(params));
            }
            if (aqlQueryElement instanceof OperatorQueryElement) {
                AqlOperatorEnum operatorEnum = ((OperatorQueryElement) aqlQueryElement).getOperatorEnum();
                condition.append(" ").append(operatorEnum.name());
            }
            if (aqlQueryElement instanceof OpenParenthesisAqlElement) {
                condition.append("(");
            }
            if (aqlQueryElement instanceof CloseParenthesisAqlElement) {
                condition.append(")");
            }
        }
        return new Pair(condition.toString(), params);
    }

    public String sort(AqlQuery aqlQuery) {
        SortDetails sortDetails = aqlQuery.getSort();
        if (sortDetails == null || sortDetails.getFields().size() == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" ORDER BY ");
        List<AqlField> fields = sortDetails.getFields();
        Iterator<AqlField> iterator = fields.iterator();
        while (iterator.hasNext()) {
            AqlField sortField = iterator.next();
            AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(sortField);
            SqlTable table = tablesLinksMap.get(extension.table).getTable();
            stringBuilder.append(table.getAlias()).append(extension.tableField);
            if (iterator.hasNext()) {
                stringBuilder.append(",");
            }
        }
        stringBuilder.append(" ").append(sortDetails.getSortType().getSqlName());
        return stringBuilder.toString();
    }

    /**
     * Query performance optimisation:
     * In case of single table join such as multiple properties table join
     * without the usage of any other table we can use inner join for better performance.
     * @param allTables
     * @return
     */
    private AqlJoinTypeEnum resolveJoinType(Iterable<SqlTable> allTables) {
        Iterable<SqlTableEnum> tables = Iterables.transform(allTables, toTableEnum);
        HashSet<SqlTableEnum> tableEnums = Sets.newHashSet();
        for (SqlTableEnum table : tables) {
            if (table != null) {
                tableEnums.add(table);
            }
        }
        if (tableEnums.size() == 1) {
            return AqlJoinTypeEnum.innerJoin;
        } else {
            return AqlJoinTypeEnum.leftOuterJoin;
        }
    }

    protected abstract SqlTableEnum getMainTable();

    Function<AqlField, SqlTable> toTables = new Function<AqlField, SqlTable>() {
        @Nullable
        @Override
        public SqlTable apply(@Nullable AqlField input) {
            AqlFieldExtensionEnum extension = AqlFieldExtensionEnum.getExtensionFor(input);
            return tablesLinksMap.get(input != null ? extension.table : null).getTable();
        }
    };
    Function<AqlQueryElement, SqlTable> firstTableFromCriteria = new Function<AqlQueryElement, SqlTable>() {
        @Nullable
        @Override
        public SqlTable apply(@Nullable AqlQueryElement input) {
            return input != null ? ((Criteria) input).getTable1() : null;
        }
    };
    Function<AqlQueryElement, SqlTable> secondTableFromCriteria = new Function<AqlQueryElement, SqlTable>() {
        @Nullable
        @Override
        public SqlTable apply(@Nullable AqlQueryElement input) {
            return input != null ? ((Criteria) input).getTable2() : null;
        }
    };
    Predicate<SqlTable> noneProperties = new Predicate<SqlTable>() {
        @Override
        public boolean apply(@Nullable SqlTable input) {
            return SqlTableEnum.node_props != (input != null ? input.getTable() : null);
        }
    };
    Predicate<SqlTable> properties = new Predicate<SqlTable>() {
        @Override
        public boolean apply(@Nullable SqlTable input) {
            return SqlTableEnum.node_props == (input != null ? input.getTable() : null);
        }
    };
    Predicate<SqlTable> notNull = new Predicate<SqlTable>() {
        @Override
        public boolean apply(@Nullable SqlTable input) {
            return input != null;
        }
    };

    Predicate<AqlQueryElement> criteriasOnly = new Predicate<AqlQueryElement>() {

        @Override
        public boolean apply(@Nullable AqlQueryElement input) {
            return input instanceof Criteria;
        }
    };
    Function<SqlTable, SqlTableEnum> toTableEnum = new Function<SqlTable, SqlTableEnum>() {
        @Nullable
        @Override
        public SqlTableEnum apply(@Nullable SqlTable input) {
            return input != null ? input.getTable() : null;
        }
    };
}
