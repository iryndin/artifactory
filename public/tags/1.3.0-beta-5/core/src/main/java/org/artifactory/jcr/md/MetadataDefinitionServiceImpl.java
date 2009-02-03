/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr.md;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.collections15.map.FastHashMap;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;

/**
 * @author freds
 * @date Sep 3, 2008
 */
@Service
public class MetadataDefinitionServiceImpl implements MetadataDefinitionService {
    private static final Logger LOGGER =
            LogManager.getLogger(MetadataDefinitionServiceImpl.class);

    private final Map<Class, MetadataDefinition> mdDefsByClass =
            new FastHashMap<Class, MetadataDefinition>(3);
    private final Map<String, MetadataDefinition> mdDefsByName =
            new FastHashMap<String, MetadataDefinition>(3);
    private final XStream xstream = new XStream();

    public XStream getXstream() {
        return xstream;
    }

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addPostInit(MetadataDefinitionService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{InternalRepositoryService.class};
    }

    public void init() {
        createMetadataDefinition(FolderInfo.class);
        createMetadataDefinition(FileInfo.class);
        createMetadataDefinition(StatsInfo.class);
    }

    public MetadataDefinition getMetadataDefinition(Class clazz) {
        MetadataDefinition definition = mdDefsByClass.get(clazz);
        if (definition == null) {
            LOGGER.warn("Creating new Metadata on the fly for: '" + clazz +
                    "'. Should have been initialized!");
            definition = createMetadataDefinition(clazz);
        }
        return definition;
    }

    private MetadataDefinition createMetadataDefinition(Class clazz) {
        MetadataDefinition definition;
        String mdName = xmlRootNameForClass(clazz);
        definition = new MetadataDefinition(mdName, clazz);
        mdDefsByClass.put(clazz, definition);
        mdDefsByName.put(mdName, definition);
        return definition;
    }

    public MetadataDefinition getMetadataDefinition(String metadataName) {
        MetadataDefinition definition = mdDefsByName.get(metadataName);
        if (definition == null) {
            LOGGER.warn("Creating new Metadata on the fly for: '" + metadataName +
                    "'. Should have been initialized!");
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
