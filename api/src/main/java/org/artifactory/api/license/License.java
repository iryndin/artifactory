/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.api.license;

import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A license object containing a license property map
 *
 * @author Noam Tenne
 */
public class License {

    private String key;
    private Date startDate;
    private Map<String, String> properties;

    public License(String key, Date startDate, String decrypted) {
        this(key, startDate);
        setDecryptedLicense(decrypted);
    }

    public License(String key, Date startDate) {
        this.key = key;
        this.startDate = startDate;
        this.properties = new HashMap<String, String>();
    }

    public String getKey() {
        return key;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String get(String key) {
        return properties.get(key);
    }

    public void put(String key, String value) {
        properties.put(key, value);
    }

    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<String> keyIterator = properties.keySet().iterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            String value = properties.get(key);
            builder.append(key).append("|").append(value);
            if (keyIterator.hasNext()) {
                builder.append("||");
            }
        }

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof License)) {
            return false;
        }

        License license = (License) o;

        if (key != null ? !key.equals(license.key) : license.key != null) {
            return false;
        }
        if (properties != null ? !properties.equals(license.properties) : license.properties != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    public boolean isValid() {
        for (RequiredProperties property : RequiredProperties.values()) {
            String currentPropertyName = property.getPropertyName();
            if (!contains(currentPropertyName)) {
                return false;
            }

            String value = get(currentPropertyName);
            if (StringUtils.isEmpty(value)) {
                return false;
            }
        }

        return true;
    }

    private void setDecryptedLicense(String decrypted) {
        String[] pairs = decrypted.split("\\|\\|");
        for (String pair : pairs) {
            String[] splitPair = pair.split("\\|");
            if (splitPair.length == 2) {
                put(splitPair[0], splitPair[1]);
            }
        }
    }
}