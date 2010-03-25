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

package org.artifactory.api.search.deployable;

import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.search.SearchResultBase;

/**
 * Holds deployable unit search result data.
 *
 * @author Noam Y. Tenne
 */
public class DeployableUnitSearchResult extends SearchResultBase {

    private DeployableUnit deployableUnit;

    /**
     * Main constructor
     *
     * @param deployableUnit Deployable unit
     */
    public DeployableUnitSearchResult(DeployableUnit deployableUnit) {
        super(null);
        this.deployableUnit = deployableUnit;
    }

    /**
     * Returns the deployable unit
     *
     * @return Deployable unit
     */
    public DeployableUnit getDeployableUnit() {
        return deployableUnit;
    }
}