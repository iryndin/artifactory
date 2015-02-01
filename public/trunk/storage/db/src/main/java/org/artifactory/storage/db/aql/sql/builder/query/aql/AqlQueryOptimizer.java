package org.artifactory.storage.db.aql.sql.builder.query.aql;

import com.google.common.collect.Lists;
import org.artifactory.aql.AqlFieldResolver;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlItemTypeEnum;
import org.artifactory.aql.model.AqlValue;
import org.artifactory.aql.model.AqlVariable;
import org.artifactory.aql.model.AqlVariableTypeEnum;
import org.artifactory.storage.db.aql.sql.builder.links.TableLink;
import org.artifactory.storage.db.aql.sql.builder.query.sql.SqlTable;
import org.artifactory.storage.db.aql.sql.builder.query.sql.type.AqlTableGraph;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;

import java.util.ArrayList;
import java.util.List;

import static org.artifactory.aql.model.AqlFieldEnum.itemType;

/**
 * @author Gidi Shabat
 */
public class AqlQueryOptimizer {
    public void optimize(AqlQuery aqlQuery) {
        boolean change;
        do {
            // Remove Item type = all criterias
            change = removeUnnecessaryItemType(aqlQuery);
            // Remove duplicates operators
            change = change | removeDuplicateOperators(aqlQuery);
            // Remove last operator
            change = change | removeLastOperator(aqlQuery);
            // Remove last operator
            change = change | removeFirstOperator(aqlQuery);
            // Remove operator before close parenthesis
            change = change | removeOperatorBeforeCloseParenthesis(aqlQuery);
            // Remove operator after open parenthesis
            change = change | removeOperatorAfterOpenParenthesis(aqlQuery);
            // Remove parenthesis with operators
            change = change | removeEmptyParenthesisWithOperator(aqlQuery);
            // Remove empty parenthesis
            change = change | removeEmptyParenthesis(aqlQuery);
        } while (change);

    }

    private boolean removeUnnecessaryItemType(AqlQuery aqlQuery) {
        ItemTypeHandleEnum itemTypeInstances = findItemTypeInstances(aqlQuery);
        // If the criteria doesn't contain "ANY" criteria, no need for optimization
        if (ItemTypeHandleEnum.none == itemTypeInstances || ItemTypeHandleEnum.fileFolder == itemTypeInstances) {
            return false;
        }
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        int i;
        for (i = 0; i < aqlElements.size(); i++) {
            AqlQueryElement aqlQueryElement = aqlElements.get(i);
            if (aqlQueryElement instanceof SimpleCriteria) {
                AqlField field = (AqlField) ((SimpleCriteria) aqlQueryElement).getVariable1();
                AqlValue value = (AqlValue) ((SimpleCriteria) aqlQueryElement).getVariable2();
                if (itemType == field.getFieldEnum() && ((Integer) value.toObject()) == AqlItemTypeEnum.any.type) {
                    break;
                }
            }
        }
        SimpleCriteria criteria = (SimpleCriteria) aqlElements.remove(i);
        if (ItemTypeHandleEnum.all == itemTypeInstances) {
            List<AqlDomainEnum> subDomains = criteria.getSubDomains();
            AqlField itemType = AqlFieldResolver.resolve(AqlFieldEnum.itemType);
            AqlVariable files = AqlFieldResolver.resolve("file", AqlVariableTypeEnum.itemType);
            AqlVariable folders = AqlFieldResolver.resolve("folder", AqlVariableTypeEnum.itemType);
            TableLink tableLink = AqlTableGraph.tablesLinksMap.get(SqlTableEnum.nodes);
            SqlTable table = tableLink.getTable();
            SimpleCriteria criteria1 = new SimpleCriteria(subDomains, itemType, table,
                    AqlComparatorEnum.equals.signature, files, table);
            SimpleCriteria criteria2 = new SimpleCriteria(subDomains, itemType, table,
                    AqlComparatorEnum.equals.signature, folders, table);
            aqlElements.add(i, AqlAdapter.close);
            aqlElements.add(i, criteria2);
            aqlElements.add(i, AqlAdapter.or);
            aqlElements.add(i, criteria1);
            aqlElements.add(i, AqlAdapter.open);
        }
        return true;
    }

    private boolean removeOperatorBeforeCloseParenthesis(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        ArrayList<Integer> toRemove = Lists.newArrayList();
        AqlQueryElement prev = null;
        AqlQueryElement current;
        for (int i = 0; i < aqlElements.size(); i++) {
            current = aqlElements.get(i);
            if (current != null && prev != null) {
                if (prev.isOperator() && current instanceof CloseParenthesisAqlElement) {
                    // Invert the order, it will be easier to remove.
                    toRemove.add(0, i - 1);
                }
            }
            prev = current;
        }
        // Remove empty parenthesis from list
        for (Integer index : toRemove) {
            aqlElements.remove(index.intValue());
            change = true;
        }
        return change;
    }

    private boolean removeOperatorAfterOpenParenthesis(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        ArrayList<Integer> toRemove = Lists.newArrayList();
        AqlQueryElement prev = null;
        AqlQueryElement current;
        for (int i = 0; i < aqlElements.size(); i++) {
            current = aqlElements.get(i);
            if (current != null && prev != null) {
                if (prev instanceof OpenParenthesisAqlElement && current.isOperator()) {
                    // Invert the order, it will be easier to remove.
                    toRemove.add(0, i);
                }
            }
            prev = current;
        }
        // Remove empty parenthesis from list
        for (Integer index : toRemove) {
            aqlElements.remove(index.intValue());
            change = true;
        }
        return change;
    }

    private boolean removeEmptyParenthesis(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        AqlQueryElement prev = null;
        AqlQueryElement current;
        while (true) {
            ArrayList<Integer> toRemove = Lists.newArrayList();
            boolean found = false;
            for (int i = 0; i < aqlElements.size(); i++) {
                current = aqlElements.get(i);
                if (current != null && prev != null) {
                    if (prev instanceof OpenParenthesisAqlElement && current instanceof CloseParenthesisAqlElement) {
                        // Invert the order, it will be easier to remove.
                        toRemove.add(0, i - 1);
                        toRemove.add(0, i);
                        found = true;
                    }
                }
                prev = current;
            }
            if (!found) {
                break;
            }
            // Remove empty parenthesis from list
            for (Integer index : toRemove) {
                aqlElements.remove(index.intValue());
                change = true;
            }
        }
        return change;
    }

    private boolean removeEmptyParenthesisWithOperator(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        ArrayList<Integer> toRemove = Lists.newArrayList();
        AqlQueryElement first = null;
        AqlQueryElement second = null;
        AqlQueryElement third;
        for (int i = 0; i < aqlElements.size(); i++) {
            third = aqlElements.get(i);
            if (first != null && second != null && third != null) {
                if (first instanceof OpenParenthesisAqlElement && second instanceof OperatorQueryElement && third instanceof CloseParenthesisAqlElement) {
                    // Invert the order, it will be easier to remove.
                    toRemove.add(0, i - 1);
                }
            }
            first = second;
            second = third;
        }
        // Remove empty parenthesis from list
        for (Integer index : toRemove) {
            aqlElements.remove(index.intValue());
            change = true;
        }
        return change;
    }

    private boolean removeDuplicateOperators(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        ArrayList<Integer> toRemove = Lists.newArrayList();
        AqlQueryElement prev = null;
        AqlQueryElement current;
        for (int i = 0; i < aqlElements.size(); i++) {
            current = aqlElements.get(i);
            if (current != null && prev != null) {
                if (prev.isOperator() && current.isOperator()) {
                    // Invert the order, it will be easier to remove.
                    toRemove.add(0, i - 1);
                }
            }
            prev = current;
        }
        // Remove empty parenthesis from list
        for (Integer index : toRemove) {
            aqlElements.remove(index.intValue());
            change = true;
        }
        return change;
    }

    private boolean removeLastOperator(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        if (aqlElements.size() > 1) {
            AqlQueryElement aqlQueryElement = aqlElements.get(aqlElements.size() - 1);
            if (aqlQueryElement.isOperator()) {
                aqlElements.remove(aqlElements.size() - 1);
                change = true;
            }
        }
        return change;
    }

    private boolean removeFirstOperator(AqlQuery aqlQuery) {
        boolean change = false;
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        if (aqlElements.size() > 1) {
            AqlQueryElement aqlQueryElement = aqlElements.get(0);
            if (aqlQueryElement.isOperator()) {
                aqlElements.remove(0);
                change = true;
            }
        }
        return change;
    }

    private ItemTypeHandleEnum findItemTypeInstances(AqlQuery aqlQuery) {
        List<AqlQueryElement> aqlElements = aqlQuery.getAqlElements();
        boolean foundFileOrFolder = false;
        boolean foundAny = false;
        for (AqlQueryElement aqlQueryElement : aqlElements) {
            if (aqlQueryElement instanceof SimpleCriteria) {
                AqlField field = (AqlField) ((SimpleCriteria) aqlQueryElement).getVariable1();
                AqlValue value = (AqlValue) ((SimpleCriteria) aqlQueryElement).getVariable2();
                if (itemType == field.getFieldEnum() && (((Integer) value.toObject()) == AqlItemTypeEnum.file.type ||
                        ((Integer) value.toObject()) == AqlItemTypeEnum.folder.type)) {
                    foundFileOrFolder = true;
                }
                if (itemType == field.getFieldEnum() && ((Integer) value.toObject()) == AqlItemTypeEnum.any.type) {
                    foundAny = true;
                }
            }
        }
        return ItemTypeHandleEnum.fromFlags(foundFileOrFolder, foundAny);
    }

    private enum ItemTypeHandleEnum {
        none, all, fileFolder, any;

        public static ItemTypeHandleEnum fromFlags(boolean fileFolderFlag, boolean anyFlag) {
            if (fileFolderFlag && anyFlag) {
                return all;
            } else if (fileFolderFlag) {
                return fileFolder;
            } else if (anyFlag) {
                return any;
            } else {
                return none;
            }



        }
    }
}
