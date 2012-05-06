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

package org.artifactory.logging.mbean;

import java.util.ArrayList;
import java.util.List;

/**
 * Error MBean implementation
 *
 * @author Noam Y. Tenne
 */
public class Error implements ErrorMBean {
    private List<String> errors = new ArrayList<String>();

    @Override
    public List<String> getErrors() {
        return this.errors;
    }

    @Override
    public void addError(String newError) {
        errors.add(newError);
    }

    @Override
    public void clear() {
        errors.clear();
    }
}