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

import org.artifactory.api.repo.RepoPath;

import javax.jcr.Node;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:39:02 PM
 */
public interface MetadataAware {
    String ARTIFACTORY_PREFIX = "artifactory:";
    String NODE_ARTIFACTORY_METADATA = ARTIFACTORY_PREFIX + "metadata";
    String NODE_ARTIFACTORY_XML = ARTIFACTORY_PREFIX + "xml";
    String PROP_ARTIFACTORY_LAST_MODIFIED_METADATA = ARTIFACTORY_PREFIX + "lastModifiedMetadata";

    /**
     * @return the JCR node that can have metadata
     */
    Node getMetadataContainer();

    /**
     * @return Get the absolute path of the this Metadata Aware item
     */
    String getAbsolutePath();

    RepoPath getRepoPath();

    void importInternalMetadata(MetadataDefinition definition, Object md);
}
