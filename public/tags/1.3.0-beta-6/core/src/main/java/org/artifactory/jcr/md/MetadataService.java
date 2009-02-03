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
import org.artifactory.api.repo.Lock;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.spring.ReloadableBean;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:48:44 PM
 */
public interface MetadataService extends ReloadableBean {
    @Lock(transactional = true, readOnly = true)
    List<String> getExtraXmlMetadataNames(MetadataAware obj);

    <MD> MD getXmlMetadataObject(MetadataAware obj, Class<MD> clazz);

    <MD> MD getXmlMetadataObject(MetadataAware obj, Class<MD> clazz,
            boolean createIfMissing);

    String getXmlMetadata(MetadataAware obj, String metadataName);

    @Lock(transactional = true)
    String setXmlMetadata(MetadataAware obj, Object xstreamable);

    void importXmlMetadata(MetadataAware obj, String metadataName, InputStream is, StatusHolder status);

    @Lock(transactional = true)
    void removeXmlMetadata(MetadataAware obj, String metadataName);

    @Lock(transactional = true)
    void writeRawXmlStream(MetadataAware obj, String metadataName, OutputStream out);

    void importXml(Node xmlNode, InputStream in) throws RepositoryException, IOException;

    void saveChecksums(MetadataAware metadataAware, String metadataName, Checksum[] checksums);
}
