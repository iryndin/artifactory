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

package org.artifactory.util;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.StringReader;

/**
 * @author yoavl
 */
public abstract class XmlUtils {
    private XmlUtils() {
        // utility class
    }

    public static String outputString(Document doc) {
        String convertedXml = new XMLOutputter().outputString(doc);
        return convertedXml;
    }

    public static Document parse(String xmlContent) {
        SAXBuilder sb = createSaxBuilder();
        try {
            return sb.build(new StringReader(xmlContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build dom document", e);
        }
    }

    public static Document parse(File input) {
        SAXBuilder sb = createSaxBuilder();
        try {
            return sb.build(new BufferedReader(new FileReader(input)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build dom document", e);
        }
    }

    public static Document parse(InputStream in) {
        SAXBuilder sb = createSaxBuilder();
        try {
            return sb.build(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build dom document", e);
        }
    }

    public static SAXBuilder createSaxBuilder() {
        SAXBuilder sb = new SAXBuilder();
        // don't validate and don't load dtd
        sb.setValidation(false);
        sb.setFeature("http://xml.org/sax/features/validation", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return sb;
    }
}