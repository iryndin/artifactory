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

package org.artifactory.support.core.collectors.storage;

import com.google.common.base.Strings;
import org.artifactory.api.jackson.JacksonWriter;
import org.artifactory.storage.StorageService;
import org.artifactory.storage.StorageSummaryImpl;
import org.artifactory.storage.StorageSummaryInfo;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.storage.StorageSummaryConfiguration;
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
 * Storage summary collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class StorageSummaryCollector extends AbstractGenericContentCollector<StorageSummaryConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(StorageSummaryCollector.class);

    @Autowired
    private StorageService storageService;

    public StorageSummaryCollector() {
        super("storage-summary");
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Produces content and returns it wrapped with
     * {@link org.artifactory.support.utils.StringBuilderWrapper}
     *
     * @param configuration the runtime configuration
     *
     * @return {@link org.artifactory.support.utils.StringBuilderWrapper}
     *
     * @throws java.io.IOException
     */
    @Override
    protected StringBuilderWrapper doProduceContent(StorageSummaryConfiguration configuration)
            throws IOException {
        StorageSummaryInfo storageSummaryInfo = storageService.getStorageSummaryInfo();
        StorageSummaryImpl storageSummary = new StorageSummaryImpl(storageSummaryInfo);

        if (storageSummary != null) {
            String serialized = JacksonWriter.serialize(storageSummary, true);
            if(!Strings.isNullOrEmpty(serialized))
                return new StringBuilderWrapper(serialized);
            else
                getLog().debug("No content was fetched from StorageService");
        }
        return failure();
    }

    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws org.artifactory.support.core.exceptions.BundleConfigurationException
     *         if configuration is invalid
     */
    @Override
    protected void doEnsureConfiguration(StorageSummaryConfiguration configuration)
            throws BundleConfigurationException {
        ;
    }
}
