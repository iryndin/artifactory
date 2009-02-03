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
package org.artifactory.update.v122rc0;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.update.jcr.BasicJcrExporter;
import org.artifactory.update.jcr.JcrPathUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;

/**
 * @author freds
 * @author Yossi Shaul
 * @date Aug 15, 2008
 */
public class JcrExporterImpl extends BasicJcrExporter {
    private static final Logger log = LoggerFactory.getLogger(JcrExporterImpl.class);
    public static final String PROP_ARTIFACTORY_MODIFIED_BY = "artifactory:modifiedBy";

    protected void exportRepository(File exportDir, Node repoNode, String repoKey,
            StatusHolder status) throws Exception {
        status.setStatus("Exporting repository " + repoKey, log);
        File repoExportDir = JcrPathUpdate.getRepoExportDir(exportDir, repoKey);
        ExportJcrFolder jcrFolder = new ExportJcrFolder(repoNode, repoKey);
        jcrFolder.exportTo(repoExportDir, status);
    }


    public static void fillWithGeneralMetadata(ItemInfo itemInfo, Node node) throws RepositoryException {
        // Get metadata in the safest way possible
        if (node.hasProperty(PROP_ARTIFACTORY_MODIFIED_BY)) {
            itemInfo.getExtension().setModifiedBy(node.getProperty(PROP_ARTIFACTORY_MODIFIED_BY).getString());
        } else {
            itemInfo.getExtension().setModifiedBy("export");
        }
        fillTimestamps(itemInfo, node);
    }
}
