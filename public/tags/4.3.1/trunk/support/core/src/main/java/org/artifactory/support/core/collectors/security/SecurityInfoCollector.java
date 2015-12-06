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

package org.artifactory.support.core.collectors.security;

import com.google.common.base.Strings;
import org.artifactory.api.jackson.JacksonWriter;
import org.artifactory.api.security.SecurityService;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.SecurityInfo;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.security.SecurityInfoConfiguration;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.core.collectors.AbstractGenericContentCollector;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.artifactory.support.utils.StringBuilderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Security info collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class SecurityInfoCollector extends AbstractGenericContentCollector<SecurityInfoConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(SecurityInfoCollector.class);

    @Autowired
    private SecurityService securityService;

    public SecurityInfoCollector() {
        super("security-info");
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Produces content and returns it wrapped with {@link StringBuilderWrapper}
     *
     * @param configuration the runtime configuration
     *
     * @return {@link StringBuilderWrapper}
     *
     * @throws IOException
     */
    @Override
    protected StringBuilderWrapper doProduceContent(SecurityInfoConfiguration configuration) throws IOException {
        SecurityInfo securityInfo = getSecurityData(configuration);
        if (securityInfo != null) {
            String serialized = JacksonWriter.serialize(securityInfo, true);
            if(!Strings.isNullOrEmpty(serialized))
                return new StringBuilderWrapper(serialized);
            else
                getLog().debug("No content was fetched from SecurityDescriptor");
        }
        return failure();
    }

    /**
     * Converts server entity to collectible support
     * entity according to configuration
     *
     * @param configuration
     * @return
     */
    private SecurityInfo getSecurityData(SecurityInfoConfiguration configuration) {
        SecurityInfo securityData = securityService.getSecurityData();
        if (!configuration.isHideUserDetails()) {
            return securityData;
        }

        SecurityInfo descriptor = InfoFactoryHolder.get().createSecurityInfo(
                null,
                securityData.getGroups(),
                securityData.getAcls()
        );
        descriptor.setVersion(securityData.getVersion());
        return descriptor;
    }
    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws org.artifactory.support.core.exceptions.BundleConfigurationException
     *         if configuration is invalid
     */
    @Override
    protected void doEnsureConfiguration(SecurityInfoConfiguration configuration)
            throws BundleConfigurationException {
        ;
    }
}
