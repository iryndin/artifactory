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

package org.artifactory.jcr.md;

import org.apache.commons.collections.FastHashMap;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.api.fs.InternalFolderInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.watch.Watchers;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.logging.LoggingService;
import org.artifactory.md.Properties;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author freds
 * @date Sep 3, 2008
 */
@Service
@Reloadable(beanClass = MetadataDefinitionService.class, initAfter = LoggingService.class)
public class MetadataDefinitionServiceImpl implements MetadataDefinitionService {
    private static final Logger log = LoggerFactory.getLogger(MetadataDefinitionServiceImpl.class);

    private final Map<Class, MetadataDefinition> mdDefsByClass = new FastHashMap();
    private final Map<String, MetadataDefinition> mdDefsByName = new FastHashMap();

    public void init() {
        // Internal metadata
        FolderInfoXmlProvider folderInfoXmlProvider = new FolderInfoXmlProvider();
        createMetadataDefinition(InternalFolderInfo.class, folderInfoXmlProvider,
                new FolderInfoPersistenceHandler(folderInfoXmlProvider), true);
        FileInfoXmlProvider fileInfoXmlProvider = new FileInfoXmlProvider();
        createMetadataDefinition(InternalFileInfo.class, fileInfoXmlProvider,
                new FileInfoPersistenceHandler(fileInfoXmlProvider), true);
        WatchersXmlProvider watchersXmlProvider = new WatchersXmlProvider();
        createMetadataDefinition(Watchers.class, watchersXmlProvider,
                new WatchersPersistenceHandler(watchersXmlProvider), true);

        // Additional persistent metadata
        StatsInfoXmlProvider statsInfoXmlProvider = new StatsInfoXmlProvider();
        createMetadataDefinition(StatsInfo.class, statsInfoXmlProvider,
                new StatsInfoPersistenceHandler(statsInfoXmlProvider), false);
        PropertiesXmlProvider propertiesXmlProvider = new PropertiesXmlProvider();
        createMetadataDefinition(Properties.class, propertiesXmlProvider,
                new PropertiesPersistenceHandler(propertiesXmlProvider), false);
        GenericXmlProvider mavenMetadataXmlProvider = new GenericXmlProvider(MavenNaming.MAVEN_METADATA_NAME);
        createMetadataDefinition(String.class, mavenMetadataXmlProvider,
                new GenericPersistenceHandler(mavenMetadataXmlProvider, false), false);

        ((FastHashMap) mdDefsByClass).setFast(true);
        ((FastHashMap) mdDefsByName).setFast(true);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
        mdDefsByClass.clear();
        mdDefsByName.clear();
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @SuppressWarnings({"unchecked"})
    public <T> MetadataDefinition<T> getMetadataDefinition(Class<T> clazz) {
        MetadataDefinition<T> definition = mdDefsByClass.get(clazz);
        if (definition == null) {
            throw new IllegalArgumentException("Creating new Metadata on the fly for: '" + clazz +
                    "'. Should have been initialized!");
        }
        return definition;
    }

    public MetadataDefinition getMetadataDefinition(String metadataName, boolean createIfEmpty) {
        if (StringUtils.isBlank(metadataName)) {
            throw new IllegalArgumentException("Metadata type name to locate cannot be null.");
        }

        MetadataDefinition definition = mdDefsByName.get(metadataName);
        if (definition == null && createIfEmpty) {
            JcrUtils.assertValidJcrName(metadataName);
            log.debug("Creating new Metadata definition on demand for '{}'.", metadataName);
            GenericXmlProvider xmlProvider = new GenericXmlProvider(metadataName);
            definition = new MetadataDefinition<String>(xmlProvider,
                    new GenericPersistenceHandler(xmlProvider, true), false);
        }
        return definition;
    }

    public Set<MetadataDefinition<?>> getAllMetadataDefinitions(boolean includeInternal) {
        Set<MetadataDefinition<?>> result = new HashSet<MetadataDefinition<?>>();
        Collection<MetadataDefinition> mdDefColl = mdDefsByName.values();
        for (MetadataDefinition definition : mdDefColl) {
            if (includeInternal || !definition.isInternal()) {
                result.add(definition);
            }
        }
        return result;
    }

    private <T> MetadataDefinition createMetadataDefinition(
            Class<T> clazz,
            XmlMetadataProvider<T> xmlProvider,
            MetadataPersistenceHandler<T> persistenceHandler,
            boolean internal) {
        MetadataDefinition definition = new MetadataDefinition<T>(xmlProvider, persistenceHandler, internal);
        if (clazz != String.class) {
            mdDefsByClass.put(clazz, definition);
        }
        mdDefsByName.put(definition.getMetadataName(), definition);
        return definition;
    }
}
