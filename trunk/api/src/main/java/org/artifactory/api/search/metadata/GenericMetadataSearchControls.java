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

package org.artifactory.api.search.metadata;

import org.artifactory.api.search.SearchControlsBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Fred Simon
 */
public class GenericMetadataSearchControls<T> extends SearchControlsBase {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(GenericMetadataSearchControls.class);

    public enum Operation {
        EQ, GT, LT, GTE, LTE
    }

    private final Class<T> metadataClass;
    private String propertyName;
    private Operation operation;
    private Object value;

    public GenericMetadataSearchControls(Class<T> metadataClass) {
        this.metadataClass = metadataClass;
    }

    public Class<T> getMetadataClass() {
        return metadataClass;
    }

    public boolean isEmpty() {
        return false;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
