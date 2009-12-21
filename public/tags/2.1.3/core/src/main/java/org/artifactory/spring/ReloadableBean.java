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

package org.artifactory.spring;

import org.artifactory.api.repo.Lock;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.version.CompoundVersionDetails;

/**
 * User: freds Date: Jul 21, 2008 Time: 11:43:00 AM
 */
public interface ReloadableBean {
    /**
     * This init will be called after the context is created and can be annotated with transactional propagation
     */
    @Lock(transactional = true)
    void init();

    /**
     * This is called when the configuration xml changes. It is using the same init order all beans that need to do
     * something on reload.
     *
     * @param oldDescriptor
     */
    void reload(CentralConfigDescriptor oldDescriptor);

    /**
     * Called when Artifactory is shutting down. Called in reverse order than the init order.
     */
    void destroy();

    /**
     * Run any necessary conversions to bring the system from the source version to the target version
     */
    void convert(CompoundVersionDetails source, CompoundVersionDetails target);
}
