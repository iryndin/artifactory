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
package org.artifactory.update.md.v130beta3;

import org.artifactory.api.md.MetadataEntry;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.current.MetadataReaderImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author freds
 * @date Nov 13, 2008
 */
public class MetadataReader130 extends MetadataReaderImpl {
    private static final Map<String, MetadataConverter> converters = new HashMap<String, MetadataConverter>(2) {{
        put(ArtifactoryFolderConverter.OLD_METADATA_NAME, new ArtifactoryFolderConverter());
        put(ArtifactoryFileConverter.OLD_METADATA_NAME, new ArtifactoryFileConverter());
    }};

    @Override
    protected MetadataEntry createMetadataEntry(String metadataName, String xmlContent) {
        MetadataConverter converter = converters.get(metadataName);
        if (converter != null) {
            xmlContent = MetadataConverterUtils.convertString(converter, xmlContent);
            metadataName = converter.getNewMetadataName();
        }
        MetadataEntry metadataEntry = new MetadataEntry(metadataName, xmlContent);
        return metadataEntry;
    }
}
