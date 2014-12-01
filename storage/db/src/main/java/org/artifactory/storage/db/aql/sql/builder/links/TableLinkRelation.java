package org.artifactory.storage.db.aql.sql.builder.links;

import org.artifactory.aql.model.AqlTableFieldsEnum;

/**
 * The TableLinkRelation represent link between two tables
 *
 * @author Gidi Shabat
 */
public class TableLinkRelation {
    private TableLink fromTable;
    private AqlTableFieldsEnum fromField;
    private TableLink toTable;
    private AqlTableFieldsEnum toFiled;

    public TableLinkRelation(TableLink fromTable, AqlTableFieldsEnum fromField, TableLink toTable,
            AqlTableFieldsEnum toFiled) {
        this.fromTable = fromTable;
        this.fromField = fromField;
        this.toTable = toTable;
        this.toFiled = toFiled;
    }

    public TableLink getFromTable() {
        return fromTable;
    }

    public AqlTableFieldsEnum getFromField() {
        return fromField;
    }

    public TableLink getToTable() {
        return toTable;
    }

    @Override
    public String toString() {
        return "TableLinkRelation{" +
                "fromTable=" + fromTable +
                ", toTable=" + toTable +
                '}';
    }

    public AqlTableFieldsEnum getToFiled() {
        return toFiled;
    }
}
