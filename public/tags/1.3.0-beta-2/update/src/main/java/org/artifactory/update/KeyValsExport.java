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
package org.artifactory.update;

import org.apache.commons.io.IOUtils;
import org.artifactory.ArtifactoryConstants;
import org.artifactory.keyval.KeyVals;
import org.artifactory.process.StatusHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * User: freds Date: Jun 6, 2008 Time: 10:09:38 AM
 */
public class KeyValsExport extends KeyVals {

    public void afterPropertiesSet() throws Exception {
        // Don't initialize OCM, the revision and version are coming
        // from ArtifactoryVersion objects
        setPrevVersion(VersionsHolder.getOriginalVersion().getValue());
        setPrevRevision("" + VersionsHolder.getOriginalVersion().getRevision());
        // The export should contain the original version not the destination
        setVersion(VersionsHolder.getOriginalVersion().getValue());
        setRevision("" + VersionsHolder.getOriginalVersion().getRevision());
    }

    public void exportTo(File exportDir, StatusHolder status) {
        // Will export version and repo substitute
        // TODO: Make global KeyVals more flexible and will contains all system props from ArtifactoryConstants
        //Store a properties file with the vals at the root folder
        Properties props = new Properties();
        props.put(KEY_VERSION, getVersion());
        props.put(KEY_REVISION, getRevision());
        props.putAll(ArtifactoryConstants.substituteRepoKeys);
        File file = new File(exportDir, "artifactory.properties");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            props.store(fos, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export properties.", e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }
}
