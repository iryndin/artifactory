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

import org.artifactory.io.checksum.Checksum;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.worker.WorkAction;
import org.artifactory.worker.WorkMessage;

/**
 * @author freds
 * @date Sep 18, 2008
 */
public class StoreChecksumsMessage extends WorkMessage {

    private MetadataAware metadataAware;
    private String metadataName;
    private Checksum[] checksums;

    public StoreChecksumsMessage(MetadataAware metadataAware, String metadataName, Checksum[] checksums) {
        super(WorkAction.STORE_CHECKSUMS, metadataAware.getRepoPath());
        this.metadataAware = metadataAware;

        this.metadataName = metadataName;
        this.checksums = checksums;
    }

    @Override
    public void execute() {
        // TODO: Supposed to get a write lock
        MetadataService metadataService = InternalContextHelper.get().beanForType(MetadataService.class);
        metadataService.saveChecksums(metadataAware, metadataName, checksums);
    }
}