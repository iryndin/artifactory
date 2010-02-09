/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.jcr.version.v150.xml;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.log.LoggerFactory;
import org.jdom.Attribute;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;

import java.util.List;

import static org.artifactory.version.XmlConverterUtils.getEditedComment;
import static org.artifactory.version.XmlConverterUtils.getNewLine;

/**
 * Converts the repo.xml file
 *
 * @author Noam Y. Tenne
 */
public class RepoXmlConverter {

    private static final Logger log = LoggerFactory.getLogger(RepoXmlConverter.class);

    static final String DTD = "http://www.jfrog.org/xsd/repository-1.6.dtd";
    static final String JCR_DATASTORE_CLASS = "org.apache.jackrabbit.core.data.db.DbDataStore";
    static final String DB_DATASTORE_CLASS = "org.artifactory.jcr.jackrabbit.ArtifactoryDbDataStoreImpl";
    static final String FILE_DATASTORE_CLASS = "org.artifactory.jcr.jackrabbit.ArtifactoryFileDataStoreImpl";
    static final String ANALYZER_CLASS = "org.artifactory.search.lucene.ArtifactoryAnalyzer";
    static final String EXCERPT_CLASS = "org.artifactory.search.ArchiveEntriesXmlExcerpt";
    static final String EXTRACTOR_CLASS = "org.artifactory.jcr.extractor.ArtifactoryTextExtractor";

    /**
     * Convert the doc
     *
     * @param doc       Doc to convert
     * @param isDefault True if the doc is the default repo.xml, or an optional one
     */
    public void convert(Document doc, boolean isDefault) {
        DocType docType = doc.getDocType();

        if (docType != null) {

            String systemId = docType.getSystemID();
            if (StringUtils.isNotBlank(systemId) && !DTD.equals(systemId)) {
                docType.setSystemID(DTD);
            }
        }

        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();

        convertDatastore(namespace, root, isDefault);
        convertWorkspaces(namespace, root);
    }

    /**
     * Converts the datastore element to point to the system customized
     *
     * @param namespace         Doc namespace
     * @param repositoryElement 'Repository' element
     * @param isDefault         True if the doc is the default repo.xml, or an optional one
     */
    private void convertDatastore(Namespace namespace, Element repositoryElement, boolean isDefault) {
        Element datastoreElement = repositoryElement.getChild("DataStore", namespace);
        if (datastoreElement != null) {

            Attribute classAttribute = datastoreElement.getAttribute("class", namespace);
            if (classAttribute != null) {

                String classValue = classAttribute.getValue();
                if (StringUtils.isNotBlank(classValue)) {
                    if (JCR_DATASTORE_CLASS.equals(classValue)) {
                        classAttribute.setValue(DB_DATASTORE_CLASS);
                        addComment(repositoryElement, repositoryElement.indexOf(datastoreElement));
                        log.info("Converting datastore class from '{}' to '{}'", classValue, DB_DATASTORE_CLASS);
                    } else if (isDefault && !FILE_DATASTORE_CLASS.equals(classValue) &&
                            !DB_DATASTORE_CLASS.equals(classValue)) {
                        throw new IllegalStateException("Unrecognized datastore class: " + classValue);
                    }
                }
            }
        }
    }

    /**
     * Converts the workspace element to point to new system analyzer, excerpt provider and text extractor
     *
     * @param namespace         Doc namespace
     * @param repositoryElement 'Repository' element
     */
    private void convertWorkspaces(Namespace namespace, Element repositoryElement) {
        @SuppressWarnings({"unchecked"})
        List<Element> workspaceElements = repositoryElement.getChildren("Workspace", namespace);
        for (Element element : workspaceElements) {

            Element searchIndexElement = element.getChild("SearchIndex", namespace);
            if (searchIndexElement != null) {

                boolean foundAnalyzer = false;
                boolean foundProvider = false;
                boolean foundTextExtractor = false;
                int analyzerLocation = -1;
                int providerLocation = -1;
                int textExtractorLocation = -1;

                @SuppressWarnings({"unchecked"})
                List<Element> paramElements = searchIndexElement.getChildren("param", namespace);
                for (Element paramElement : paramElements) {

                    Attribute nameAttribute = paramElement.getAttribute("name");
                    Attribute valueAttribute = paramElement.getAttribute("value");
                    if ((nameAttribute != null) && (valueAttribute != null)) {

                        String nameValue = nameAttribute.getValue();
                        String valueString = valueAttribute.getValue();
                        if (StringUtils.isNotBlank(nameValue) && StringUtils.isNotBlank(valueString)) {

                            if (!foundAnalyzer && "analyzer".equals(nameValue) &&
                                    !ANALYZER_CLASS.equals(valueString)) {

                                foundAnalyzer = true;
                                valueAttribute.setValue(ANALYZER_CLASS);
                                analyzerLocation = searchIndexElement.indexOf(paramElement);
                            } else if (!foundProvider && "excerptProviderClass".equals(nameValue) &&
                                    !EXCERPT_CLASS.equals(valueString)) {

                                foundProvider = true;
                                valueAttribute.setValue(EXCERPT_CLASS);
                                providerLocation = searchIndexElement.indexOf(paramElement);
                            } else if (!foundTextExtractor && "textFilterClasses".equals(nameValue) &&
                                    !EXTRACTOR_CLASS.equals(valueString)) {

                                foundTextExtractor = true;
                                valueAttribute.setValue(EXTRACTOR_CLASS);
                                textExtractorLocation = searchIndexElement.indexOf(paramElement);
                            }
                        }
                    }

                    if (foundAnalyzer && foundProvider && foundTextExtractor) {
                        break;
                    }
                }

                if (analyzerLocation != -1) {
                    providerLocation += 2;
                    textExtractorLocation += 2;
                    addComment(searchIndexElement, analyzerLocation);
                }

                if (providerLocation != -1) {
                    textExtractorLocation += 2;
                    addComment(searchIndexElement, providerLocation);
                }

                if (textExtractorLocation != -1) {
                    addComment(searchIndexElement, textExtractorLocation);
                }
            }
        }
    }

    /**
     * Adds a comment element
     *
     * @param element Element to add comment to
     * @param index   Index to add comment at
     */
    private void addComment(Element element, int index) {
        element.addContent(index, Lists.newArrayList(getEditedComment(), getNewLine()));
    }
}