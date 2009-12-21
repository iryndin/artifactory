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

package org.artifactory.jcr.md;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.collections15.map.FastHashMap;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.FolderAdditionalInfo;
import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.md.Properties;
import org.artifactory.api.md.watch.Watchers;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

/**
 * @author freds
 * @date Sep 3, 2008
 */
@Service
@Reloadable(beanClass = MetadataDefinitionService.class, initAfter = InternalCentralConfigService.class)
public class MetadataDefinitionServiceImpl implements MetadataDefinitionService {
    private static final Logger log = LoggerFactory.getLogger(MetadataDefinitionServiceImpl.class);

    private final Map<Class, MetadataDefinition> mdDefsByClass = new FastHashMap<Class, MetadataDefinition>(3);
    private final Map<String, MetadataDefinition> mdDefsByName = new FastHashMap<String, MetadataDefinition>(3);
    private final XStream xstream = XStreamFactory.create();

    public XStream getXstream() {
        return xstream;
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalCentralConfigService.class};
    }

    public void init() {
        // Internal xstreamables metadata
        createMetadataDefinition(FolderInfoImpl.class, false, true);
        createMetadataDefinition(FileInfoImpl.class, false, true);
        createMetadataDefinition(FolderAdditionalInfo.class, true, true);
        createMetadataDefinition(FileAdditionalInfo.class, true, true);
        createMetadataDefinition(Watchers.class, true, true);

        // Additional persistent xstreamables metadata
        createMetadataDefinition(StatsInfo.class, true, false);
        createMetadataDefinition(Properties.class, true, false);

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

    public MetadataDefinition getMetadataDefinition(Class clazz) {
        MetadataDefinition definition = mdDefsByClass.get(clazz);
        if (definition == null) {
            throw new IllegalArgumentException("Creating new Metadata on the fly for: '" + clazz +
                    "'. Should have been initialized!");
        }
        return definition;
    }

    private MetadataDefinition createMetadataDefinition(
            Class clazz, boolean persistOnMetadataContainer, boolean internal) {
        MetadataDefinition definition;
        String mdName = xmlRootNameForClass(clazz);
        definition = new MetadataDefinition(mdName, clazz, persistOnMetadataContainer, internal);
        mdDefsByClass.put(clazz, definition);
        mdDefsByName.put(mdName, definition);
        return definition;
    }

    public MetadataDefinition getMetadataDefinition(String metadataName) {
        MetadataDefinition definition = mdDefsByName.get(metadataName);
        if (definition == null) {
            log.debug("Creating new Metadata definition on demand for '{}'.", metadataName);
            definition = new MetadataDefinition(metadataName);
            mdDefsByName.put(metadataName, definition);
        }
        return definition;
    }

    public Collection<String> getAllDefinitionNames() {
        return mdDefsByName.keySet();
    }

    private String xmlRootNameForClass(Class clazz) {
        //Processing is cached by xstream (won't happen if a class was already processed)
        xstream.processAnnotations(clazz);
        String xmlNodeName = xstream.getMapper().serializedClass(clazz);
        if (xmlNodeName == null) {
            throw new IllegalArgumentException(
                    "Class '" + clazz.getName() + "' is not an XStream mapped class.");
        }
        return xmlNodeName;
    }

}
