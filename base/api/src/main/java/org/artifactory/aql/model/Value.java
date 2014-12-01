package org.artifactory.aql.model;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.aql.AqlException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Gidi Shabat
 */
public class Value implements Variable {

    private AqlVariableTypeEnum valueType;
    private String value;

    public Value(AqlVariableTypeEnum valueType, String value) {
        this.valueType = valueType;
        this.value = value;
    }

    @Override
    public AqlVariableTypeEnum getValueType() {
        return valueType;
    }

    public void setVariableType(AqlVariableTypeEnum valueType) {
        this.valueType = valueType;
    }

    public boolean isString() {
        return AqlVariableTypeEnum.string == valueType;
    }

    public Object toObject() throws AqlException {
        Object result = value;
        if (AqlVariableTypeEnum.string == valueType) {
            result = value;
        }
        if (AqlVariableTypeEnum.date == valueType) {
            CentralConfigService centralConfigService = ContextHelper.get().getCentralConfig();
            String defaultPattern = "yyyy-MM-dd HH:mm:ss";
            DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(defaultPattern);
            if (centralConfigService != null && centralConfigService.getDateFormatter() != null) {
                dateFormatter = centralConfigService.getDateFormatter();
            }
            try {
                DateTime dateTime = dateFormatter.parseDateTime(value);
                result = dateTime.toDate().getTime();
            } catch (Exception e) {
                throw new AqlException(String.format("Invalid Date format: %s please check" +
                        " Artifactory' default format or use the default format: %s ", value, defaultPattern), e);
            }
        }
        if (AqlVariableTypeEnum.longInt == valueType) {
            result = Long.valueOf(value);
        }
        if (AqlVariableTypeEnum.integer == valueType) {
            result = Integer.valueOf(value);
        }
        return result;

    }
}
