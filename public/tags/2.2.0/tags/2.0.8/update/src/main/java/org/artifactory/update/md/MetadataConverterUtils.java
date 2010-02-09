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
package org.artifactory.update.md;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Nov 11, 2008
 */
public class MetadataConverterUtils {
    private static final String CREATED_BY = "createdBy";
    private static final String MODIFIED_BY = "modifiedBy";
    private static final String REPO_PATH = "repoPath";
    private static final String REPO_KEY = "repoKey";
    private static final String PATH = "path";
    private static final String NAME = "name";
    private static final String EXTENSION = "extension";
    private static final String REL_PATH = "relPath";

    private static final String[] EXTENSION_FIELDS = {"lastUpdated", MODIFIED_BY, CREATED_BY, "sha1", "md5"};

    public static List<Element> extractExtensionFields(Element rootElement) {
        String modifiedBy = rootElement.getChildText(MODIFIED_BY);
        List<Element> toMove = new ArrayList<Element>(EXTENSION_FIELDS.length);
        for (String tagName : EXTENSION_FIELDS) {
            Element element = rootElement.getChild(tagName);
            if (element != null) {
                toMove.add(element);
                rootElement.removeChild(tagName);
            } else {
                if (CREATED_BY.equals(tagName)) {
                    toMove.add(new Element(CREATED_BY).setText(modifiedBy));
                }
            }
        }
        return toMove;
    }

    public static void addNewContent(Element rootElement, RepoPath repoPath, List<Element> toMove) {
        rootElement.addContent(new Element(NAME).setText(repoPath.getName()));
        rootElement.addContent(new Element(REPO_PATH).
                addContent(new Element(REPO_KEY).setText(repoPath.getRepoKey())).
                addContent(new Element(PATH).setText(repoPath.getPath())));
        rootElement.addContent(new Element(EXTENSION).addContent(toMove));
    }

    public static RepoPath extractRepoPath(Element rootElement) {
        String repoKey = rootElement.getChildText(REPO_KEY);
        String relPath = rootElement.getChildText(REL_PATH);
        RepoPath repoPath = new RepoPath(repoKey, relPath);
        rootElement.removeChild(REPO_KEY);
        rootElement.removeChild(REL_PATH);
        return repoPath;
    }

    public static String convertString(MetadataConverter converter, String xmlContent) {
        Document doc = ConverterUtils.parse(xmlContent);
        converter.convert(doc);
        return ConverterUtils.outputString(doc);
    }
}
