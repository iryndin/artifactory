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

package org.artifactory.descriptor.repo;

/**
 * Determines if a virtual repo should be filtered and not processed by the virtual repo resolution.
 *
 * @author Shay Yaakov
 * @see org.artifactory.descriptor.repo.VirtualRepoResolver
 */
public interface VirtualResolverFilter {

    /**
     * @param descriptor The virtual repo to check for filtering
     * @return True if the filter accepts the virtual repo according to the filter rules
     */
    boolean accepts(VirtualRepoDescriptor descriptor);
}
