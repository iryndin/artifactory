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

package org.artifactory.ui.rest.service.admin.advanced.support;

import org.artifactory.support.config.bundle.BundleConfiguration;

import javax.servlet.http.HttpServletRequest;

/**
 * A container for {@link BundleConfiguration} and
 * {@link HttpServletRequest}
 *
 * @author Michael Pasternak
 */
public class BundleConfigurationWrapper {
    private final BundleConfiguration bundleConfiguration;
    private final HttpServletRequest httpServletRequest;

    public BundleConfigurationWrapper(BundleConfiguration bundleConfiguration,
            HttpServletRequest httpServletRequest) {
        this.bundleConfiguration = bundleConfiguration;
        this.httpServletRequest = httpServletRequest;
    }

    public BundleConfiguration getBundleConfiguration() {
        return bundleConfiguration;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }
}
