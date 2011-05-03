/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.cli.common;

/**
 * The Parameter class interface
 *
 * @author Noam Tenne
 */
public interface Param {

    /**
     * Returns the parameter description
     *
     * @return String Parameter description
     */
    String getDescription();

    /**
     * Returns an indication of the need for an extra parameter
     *
     * @return boolean Needs extra parameter
     */
    boolean isNeedExtraParam();

    /**
     * Returns the parameter description
     *
     * @return String Parameter description
     */
    String getParamDescription();

    /**
     * Returns the parameter name
     *
     * @return String parameter name
     */
    String getName();

    /**
     * Sets the value of the extra parameter
     *
     * @param value Value of extra parameter
     */
    void setValue(String value);

    /**
     * Returns the value of the extra parameter
     *
     * @return String Extra paramter value
     */
    String getValue();

    /**
     * Returns an indication of the validness of the extra parameter value
     *
     * @return boolean Validness of extra parameter value
     */
    boolean isSet();

    /**
     * Sets default value for the extra parameter
     */
    void set();
}
