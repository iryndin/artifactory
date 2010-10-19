/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.api.md;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A map of stringified keys and values, used for storing arbitrary key-value metadata on repository items.
 *
 * @author Yoav Landman
 */
@XStreamAlias(Properties.ROOT)
public class PropertiesImpl implements Properties {
    private static final Logger log = LoggerFactory.getLogger(PropertiesImpl.class);

    private final SetMultimap<String, String> props;

    public PropertiesImpl() {
        props = LinkedHashMultimap.create();
    }

    public PropertiesImpl(Properties m) {
        //props = LinkedHashMultimap.create( m.props);
        props = LinkedHashMultimap.create();
        Set<Map.Entry<String, String>> entries = m.entries();
        for (Map.Entry<String, String> entry : entries) {
            props.put(entry.getKey(), entry.getValue());
        }
    }

    public int size() {
        return props.size();
    }

    @Nullable
    public Set<String> get(@Nonnull String key) {
        return props.get(key);
    }

    public boolean putAll(@Nonnull String key, Iterable<? extends String> values) {
        return props.putAll(key, values);
    }

    public boolean putAll(Multimap<? extends String, ? extends String> multimap) {
        return props.putAll(multimap);
    }

    public void clear() {
        props.clear();
    }

    public Set<String> removeAll(@Nonnull Object key) {
        return props.removeAll(key);
    }

    public boolean put(String key, String value) {
        return props.put(key, value);
    }

    public Collection<String> values() {
        return props.values();
    }

    public Set<Map.Entry<String, String>> entries() {
        return props.entries();
    }

    public Multiset<String> keys() {
        return props.keys();
    }

    public Set<String> keySet() {
        return props.keySet();
    }

    public boolean isEmpty() {
        return props.isEmpty();
    }

    public boolean containsKey(String key) {
        return props.containsKey(key);
    }

    public MatchResult matchQuery(Properties queryProperties) {
        if (queryProperties == null) {
            return MatchResult.NO_MATCH;
        }
        for (String qPropKey : queryProperties.keySet()) {
            //Hack - need to model query properties together with their control flags
            boolean mandatory = false;
            String propKey = qPropKey;
            if (qPropKey != null && qPropKey.endsWith(MANDATORY_SUFFIX)) {
                mandatory = true;
                propKey = qPropKey.substring(0, qPropKey.length() - MANDATORY_SUFFIX.length());
            }

            //If the key given from the query must exist
            if (mandatory) {

                //If the current properties contain the given key
                if (containsKey(propKey)) {

                    Set<String> queryPropertyValues = clearBlankAndReturnPropertyValues(queryProperties.get(qPropKey));

                    //Only check the current property values if the request property was given with values
                    if (!queryPropertyValues.isEmpty()) {

                        //The given query properties have a value, so we should try to match
                        Set<String> currentPropertyValue = clearBlankAndReturnPropertyValues(get(propKey));
                        if (!queryPropertyValues.equals(currentPropertyValue)) {

                            //The properties don't match
                            return MatchResult.CONFLICT;
                        }
                    }
                } else {
                    //Conflict since the key given from the properties is mandatory and doesn't exist in the current properties
                    return MatchResult.CONFLICT;
                }
            } else {

                Set<String> queryPropertyValues = clearBlankAndReturnPropertyValues(queryProperties.get(qPropKey));

                if (!queryPropertyValues.isEmpty()) {
                    //If the current properties contain the given query property key
                    if (containsKey(propKey)) {

                        //The given query properties have a value, so we should try to match
                        Set<String> currentPropertyValue = clearBlankAndReturnPropertyValues(get(propKey));

                        if (!queryPropertyValues.equals(currentPropertyValue)) {

                            //The properties conflict
                            return MatchResult.CONFLICT;
                        }
                    } else {
                        //The current property doesn't have the given query property, so it does not conflict either
                        return MatchResult.NO_MATCH;
                    }
                }
            }
        }
        return MatchResult.MATCH;
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (that instanceof PropertiesImpl) {
            PropertiesImpl otherProps = (PropertiesImpl) that;
            return this.props.equals(otherProps.props);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return props.hashCode();
    }

    @Override
    public String toString() {
        return props.toString();
    }

    /**
     * Extracts the matrix params from the given strings and adds them to the properties object.<br> Note that the
     * matrix params string must begin with matrix params, and any params found are omitted, so only the rest of the
     * path (if exists) will remain in the string.
     *
     * @param propertyCollection Property collection to append to. Cannot be null
     * @param matrixParams       Matrix params to process. Cannot be null
     */
    public static void processMatrixParams(Properties propertyCollection, String matrixParams) {
        int matrixParamStart = 0;
        do {
            int matrixParamEnd = matrixParams.indexOf(MATRIX_PARAMS_SEP, matrixParamStart + 1);
            if (matrixParamEnd < 0) {
                matrixParamEnd = matrixParams.length();
            }
            String param = matrixParams.substring(matrixParamStart + 1, matrixParamEnd);
            int equals = param.indexOf('=');
            if (equals > 0) {
                String key = param.substring(0, equals);
                String value = param.substring(equals + 1);
                // url-decode the value
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("Encoding not supported: {}. Using original value", e.getMessage());
                }
                propertyCollection.put(key, value);
            } else if (param.length() > 0) {
                propertyCollection.put(param, "");
            } // else no key declared, ignore
            matrixParamStart = matrixParamEnd;
        } while (matrixParamStart > 0 && matrixParamStart < matrixParams.length());
    }

    /**
     * Encodes the given properties ready to attach to an HTTP request
     *
     * @param requestProperties Properties to encode. Can be null
     * @return HTTP request ready property chain
     */
    public static String encodeForRequest(Properties requestProperties) throws UnsupportedEncodingException {
        StringBuilder requestPropertyBuilder = new StringBuilder();
        if (requestProperties != null) {
            for (Map.Entry<String, String> requestPropertyEntry : requestProperties.entries()) {
                requestPropertyBuilder.append(Properties.MATRIX_PARAMS_SEP);

                String key = requestPropertyEntry.getKey();
                boolean isMandatory = false;
                if (key.endsWith(Properties.MANDATORY_SUFFIX)) {
                    key = key.substring(0, key.length() - 1);
                    isMandatory = true;
                }
                requestPropertyBuilder.append(URLEncoder.encode(key, "utf-8"));
                if (isMandatory) {
                    requestPropertyBuilder.append("+");
                }
                String value = requestPropertyEntry.getValue();
                if (StringUtils.isNotBlank(value)) {
                    requestPropertyBuilder.append("=").append(URLEncoder.encode(value, "utf-8"));
                }
            }
        }

        return requestPropertyBuilder.toString();
    }

    /**
     * Returns a copy of the given property value set after clearing any blank\null values it might contain
     *
     * @param propertyValues Property value set. Can be null
     * @return Copy of given set without the null\blank values or an Empty set if given a null set
     */
    private Set<String> clearBlankAndReturnPropertyValues(Set<String> propertyValues) {
        Set<String> clearedPropertyValues = Sets.newHashSet();
        if (propertyValues == null) {
            return clearedPropertyValues;
        }

        for (String propertyValue : propertyValues) {
            if (StringUtils.isNotBlank(propertyValue)) {
                clearedPropertyValues.add(propertyValue);
            }
        }

        return clearedPropertyValues;
    }
}