package org.artifactory.aql;

import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlVariableTypeEnum;
import org.artifactory.aql.model.Field;
import org.artifactory.aql.model.Value;
import org.artifactory.aql.model.Variable;
import org.artifactory.util.Pair;

/**
 * @author Gidi Shabat
 */
public class FieldResolver {
    public static Variable resolve(String fieldName) {
        if (StringUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("Cannot accept null or empty property name!");
        }
        AqlField value = AqlField.value(fieldName);
        if (value != null) {
            return new Field(value);
        } else {
            return new Value(AqlVariableTypeEnum.string, fieldName);
        }
    }

    public static Pair<Variable, Variable> resolve(String firstFieldName, String secondFieldName)
            throws AqlException {
        Variable first = resolve(firstFieldName);
        if (first instanceof Field ) {
            return new Pair<>(first, (Variable)new Value(first.getValueType(),secondFieldName));
        }
        if (first instanceof Value ) {
            return new Pair<>(first,  (Variable)new Value(AqlVariableTypeEnum.string,secondFieldName));
        }
        throw new AqlException("Should not reach to this point of code");
    }


}
