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

package org.artifactory.jcr.version.v150;

import org.artifactory.jcr.JcrConfResourceLoader;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.spring.ArtifactoryStorageContext;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.update.FatalConversionException;
import org.artifactory.version.converter.ConfigurationConverter;

import javax.jcr.Session;

/**
 * @author freds
 */
public class JcrMetadataConverter implements ConfigurationConverter<Session> {

    @Override
    public void convert(Session jcrSession) {
        ArtifactoryStorageContext context = StorageContextHelper.get();
        JcrService jcrService;

        try {
            // First need the JcrService to be initialized
            jcrService = context.getJcrService();
            jcrService.init();

            // Then execute the JCR DB conversion
            JcrMetadataConverterExecutor executor = new JcrMetadataConverterExecutor(jcrSession);
            executor.convert();

            // Shutdown the JCR repo
            jcrService.destroy();

            // Clean the index folder to enforce full re-indexing
            IndexRemover remover = new IndexRemover();
            remover.convert(context.getArtifactoryHome());
        } catch (Exception e) {
            if (e instanceof FatalConversionException) {
                throw (FatalConversionException) e;
            }
            throw new FatalConversionException(
                    "Unexpected exception during critical migration. Stopping the loading process due to " +
                            e.getMessage(), e);
        }

        try {
            // Discard the transient repo xml
            JcrConfResourceLoader.removeTransientRepoXml();
            // Make sure the repo.xml loader is closed for full re-init
            ResourceStreamHandle jcrConfBean = (ResourceStreamHandle) context.getBean("repoXmlResource");
            jcrConfBean.close();
            // Full reinitialization of JCR Repository
            jcrService.reCreateJcrRepository();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RepositoryRuntimeException(e);
        }
    }
}
