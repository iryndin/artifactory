/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.api.search.xml.metadata;

import org.artifactory.api.search.SearchControlsBase;

/**
 * @author Fred Simon
 */
public class GenericMetadataSearchControls<T> extends SearchControlsBase {

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

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isWildcardsOnly() {
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
