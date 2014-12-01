package org.artifactory.aql.model;

/**
 * @author Gidi Shabat
 */
public class Field implements Variable {
    private AqlField fieldEnum;

    public Field(AqlField fieldEnum) {
        this.fieldEnum = fieldEnum;
    }

    public AqlField getFieldEnum() {
        return fieldEnum;
    }

    @Override
    public AqlVariableTypeEnum getValueType() {
        return fieldEnum.type;
    }
}
