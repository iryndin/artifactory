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

package org.artifactory.config;

import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.interceptor.Interceptors;
import org.artifactory.spring.InternalContextHelper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Central config interceptors chain manager.
 *
 * @author Yossi Shaul
 */
@Service
public class ConfigurationChangesInterceptorsImpl extends Interceptors<ConfigurationChangesInterceptor>
        implements ConfigurationChangesInterceptors {
    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(ConfigurationChangesInterceptors.class);
    }

    public void onBeforeSave(CentralConfigDescriptor newDescriptor) {
        for (ConfigurationChangesInterceptor interceptor : this) {
            interceptor.onBeforeSave(newDescriptor);
        }
    }
}