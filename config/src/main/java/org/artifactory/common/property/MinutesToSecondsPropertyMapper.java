package org.artifactory.common.property;

import org.artifactory.log.BootstrapLogger;

/**
 * Maps the value of a property from minutes to its counterpart in seconds
 *
 * @author Tomer Cohen
 */
public class MinutesToSecondsPropertyMapper extends PropertyMapperBase {

    public MinutesToSecondsPropertyMapper(String origPropertyName) {
        super(origPropertyName);
    }

    public String map(String origValue) {
        int valueInMinutes;
        try {
            valueInMinutes = Integer.parseInt(origValue);
        } catch (NumberFormatException e) {
            String msg = "'" + origValue + "' is an illegal value.";
            BootstrapLogger.error(msg);
            throw new IllegalArgumentException(msg, e);
        }
        return String.valueOf(valueInMinutes * 60);
    }
}
