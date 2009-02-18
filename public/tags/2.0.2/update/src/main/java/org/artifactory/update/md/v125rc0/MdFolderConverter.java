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
package org.artifactory.update.md.v125rc0;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.MetadataType;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * @author freds
 * @date Nov 11, 2008
 */
public class MdFolderConverter implements MetadataConverter {
    static final String ARTIFACTORY_NAME = "artifactoryName";

    public String getNewMetadataName() {
        return "artifactory-folder";
    }

    public MetadataType getSupportedMetadataType() {
        return MetadataType.folder;
    }

    public void convert(Document doc) {
        Element rootElement = doc.getRootElement();
        rootElement.setName(getNewMetadataName());
        RepoPath repoPath = MetadataConverterUtils.extractRepoPath(rootElement);
        // In this version the relPath is the father and name need to be added
        if (rootElement.getChild(ARTIFACTORY_NAME) != null) {
            String name = rootElement.getChildText(ARTIFACTORY_NAME);
            repoPath = new RepoPath(repoPath, name);
        }
        List<Element> toMove = MetadataConverterUtils.extractExtensionFields(rootElement);
        MetadataConverterUtils.addNewContent(rootElement, repoPath, toMove);
        // Not used anymore
        rootElement.removeChild(ARTIFACTORY_NAME);
    }
}
