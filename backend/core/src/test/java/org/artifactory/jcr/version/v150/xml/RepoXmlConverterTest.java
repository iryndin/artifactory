/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.artifactory.convert.XmlConverterTest;
import org.artifactory.util.ResourceUtils;
import org.artifactory.util.XmlUtils;
import org.jdom.Attribute;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.testng.Assert.*;

/**
 * Tests the behavior of the repo xml converter
 *
 * @author Noam Y. Tenne
 */
public class RepoXmlConverterTest extends XmlConverterTest {

    /**
     * Test conversion of a valid repo.xml
     */
    @Test
    public void testConvert() throws Exception {
        InputStream is = ResourceUtils.getResource("/org/artifactory/jcr/version/v150/xml/repo.xml");
        Document doc = XmlUtils.parse(is);
        RepoXmlConverter xmlConverter = new RepoXmlConverter();
        xmlConverter.convert(doc, true);

        assertConvertedDoc(doc);
    }

    /**
     * Test conversion of a repo.xml with an unrecognized datastore
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void testConvertConfigWithCustomDatastore() throws Exception {
        InputStream is = ResourceUtils.getResource("/org/artifactory/jcr/version/v150/xml/repo-custom-ds.xml");
        Document doc = XmlUtils.parse(is);
        RepoXmlConverter xmlConverter = new RepoXmlConverter();
        xmlConverter.convert(doc, true);

        assertConvertedDoc(doc);
    }

    /**
     * Tests the converted document. This method is called from another tests so check before modifying it.
     *
     * @param doc Doc to test
     */
    public void assertConvertedDoc(Document doc) {
        DocType docType = doc.getDocType();

        assertNotNull(docType, "repo.xml 'DocType' element cannot be null.");
        String systemId = docType.getSystemID();

        assertTrue(isNotBlank(systemId), "repo.xml 'DocType' system ID cannot be blank.");
        assertEquals(systemId, RepoXmlConverter.DTD, "repo.xml 'DocType' system ID should have been converted.");

        Element root = doc.getRootElement();
        Namespace namespace = root.getNamespace();

        testDatastore(namespace, root);
        testWorkspaces(namespace, root);
    }

    /**
     * Tests that the datastore element points to the custom system datastore
     *
     * @param namespace         Doc namespace
     * @param repositoryElement 'Repository' element
     */
    private void testDatastore(Namespace namespace, Element repositoryElement) {
        Element datastoreElement = repositoryElement.getChild("DataStore", namespace);
        assertNotNull(repositoryElement, "repo.xml 'Datastore' element cannot be null.");

        Attribute classAttribute = datastoreElement.getAttribute("class", namespace);
        assertNotNull(repositoryElement, "repo.xml 'class' attribute cannot be null.");

        String classValue = classAttribute.getValue();
        assertTrue(isNotBlank(classValue), "repo.xml 'class' attribute value cannot be blank.");
        assertEquals(classValue, RepoXmlConverter.DB_DATASTORE_CLASS,
                "Artifactory DB datastore should have been converted");
    }

    /**
     * Tests that the workspace element is configured with the new system analyzer, excerpt provider and text extractor
     *
     * @param namespace         Doc namespace
     * @param repositoryElement 'Repository' element
     */
    private void testWorkspaces(Namespace namespace, Element repositoryElement) {
        @SuppressWarnings({"unchecked"})
        List<Element> workspaceElements = repositoryElement.getChildren("Workspace", namespace);
        for (Element element : workspaceElements) {

            Element searchIndexElement = element.getChild("SearchIndex", namespace);
            assertNotNull(searchIndexElement, "repo.xml 'SearchIndex' attribute cannot be null.");

            boolean foundAnalyzer = false;
            boolean foundProvider = false;
            boolean foundTextExtractor = false;

            @SuppressWarnings({"unchecked"})
            List<Element> paramElements = searchIndexElement.getChildren("param", namespace);
            for (Element paramElement : paramElements) {

                Attribute nameAttribute = paramElement.getAttribute("name");
                Attribute valueAttribute = paramElement.getAttribute("value");
                if ((nameAttribute != null) && (valueAttribute != null)) {

                    String nameValue = nameAttribute.getValue();
                    String valueString = valueAttribute.getValue();
                    if (isNotBlank(nameValue) && isNotBlank(valueString)) {

                        if (!foundAnalyzer && "analyzer".equals(nameValue) &&
                                RepoXmlConverter.ANALYZER_CLASS.equals(valueString)) {

                            foundAnalyzer = true;
                        } else if (!foundProvider && "excerptProviderClass".equals(nameValue) &&
                                RepoXmlConverter.EXCERPT_CLASS.equals(valueString)) {

                            foundProvider = true;
                        } else if (!foundTextExtractor && "textFilterClasses".equals(nameValue) &&
                                RepoXmlConverter.EXTRACTOR_CLASS.equals(valueString)) {
                            foundTextExtractor = true;
                        }
                    }
                }

                if (foundAnalyzer && foundProvider && foundTextExtractor) {
                    break;
                }
            }

            assertTrue(foundAnalyzer, "Could not find artifactory analyzer declaration.");
            assertTrue(foundProvider, "Could not find archive excerpt provider declaration.");
            assertTrue(foundTextExtractor, "Could not find artifactory text extractor declaration.");
        }
    }
}