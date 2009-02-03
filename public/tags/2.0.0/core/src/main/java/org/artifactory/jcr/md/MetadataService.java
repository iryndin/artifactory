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

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.repo.Lock;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.spring.ReloadableBean;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:48:44 PM
 */
public interface MetadataService extends ReloadableBean {
    @Lock(transactional = true, readOnly = true)
    List<String> getXmlMetadataNames(MetadataAware obj);

    <MD> MD getXmlMetadataObject(MetadataAware metadataAware, Class<MD> clazz);

    <MD> MD getXmlMetadataObject(MetadataAware metadataAware, Class<MD> clazz, boolean createIfMissing);

    String getXmlMetadata(MetadataAware metadataAware, String metadataName);

    @Lock(transactional = true)
    String setXmlMetadata(MetadataAware metadataAware, Object xstreamable);

    @Lock(transactional = true)
    void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is, StatusHolder status);

    @Lock(transactional = true)
    void removeXmlMetadata(MetadataAware MetadataAware, String metadataName);

    @Lock(transactional = true)
    void writeRawXmlStream(MetadataAware metadataAware, String metadataName, OutputStream out);

    void saveChecksums(MetadataAware metadataAware, String metadataName, Checksum[] checksums);

    /**
     * Gets MetadataInfo for <i>existing</i> metadata
     *
     * @param MetadataAware
     * @param metadataName
     * @return
     */
    @Lock(transactional = true, readOnly = true)
    MetadataInfo getMetadataInfo(MetadataAware MetadataAware, String metadataName);

    @Lock(transactional = true, readOnly = true)
    boolean hasXmlMetdata(MetadataAware metadataAware, String metadataName);

    void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is);
}
