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

package org.artifactory.api.search;

import org.artifactory.common.ConstantValues;

/**
 * A value object holding specifications for a jcr query
 */
public final class JcrQuerySpec {

    private Type type;
    private String query;
    private int limit = ConstantValues.searchUserQueryLimit.getInt();

    public static JcrQuerySpec xpath(String query) {
        JcrQuerySpec spec = new JcrQuerySpec();
        spec.type = Type.XPATH;
        spec.query = query;
        return spec;
    }

    public static JcrQuerySpec sql(String query) {
        JcrQuerySpec spec = new JcrQuerySpec();
        spec.type = Type.SQL;
        spec.query = query;
        return spec;
    }

    public JcrQuerySpec limit(int limit) {
        this.limit = limit;
        return this;
    }

    public JcrQuerySpec noLimit() {
        this.limit = -1;
        return this;
    }

    public Type type() {
        return type;
    }

    public String jcrType() {
        return type.type();
    }

    public String query() {
        return query;
    }

    public int limit() {
        return limit;
    }

    public enum Type {
        //Don't introduce direct dependencies on jcr classes
        XPATH("xpath"), SQL("sql");

        private final String jcrType;

        Type(String jcrType) {
            this.jcrType = jcrType;
        }

        String type() {
            return jcrType;
        }
    }
}