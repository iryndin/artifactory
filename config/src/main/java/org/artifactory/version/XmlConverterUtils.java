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

package org.artifactory.version;

import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public class XmlConverterUtils {
    // ATTENTION: NO LOGGER IN HERE SINCE IT IS CALLED BY LOGGER CONVERTER => STACK OVERFLOW

    public static String convert(List<XmlConverter> converters, String in) {
        // If no converters nothing to do
        if (converters.isEmpty()) {
            return in;
        }
        Document doc = parse(in);
        for (XmlConverter converter : converters) {
            converter.convert(doc);
        }
        return outputString(doc);
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

    public static Document parse(InputStream in) {
        SAXBuilder sb = createSaxBuilder();
        try {
            return sb.build(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build dom document", e);
        }
    }

    /**
     * Returns a comment that states that a line was added by Artifactory's update mechanism
     *
     * @return Added comment
     */
    public static Comment getAddedComment() {
        return getChangedComment("Added");
    }

    /**
     * Returns a comment that states that a line was edited by Artifactory's update mechanism
     *
     * @return Edited comment
     */
    public static Comment getEditedComment() {
        return getChangedComment("Edited");
    }

    /**
     * Returns a "new line" element
     *
     * @return "New line" element
     */
    public static Content getNewLine() {
        return new Text("\n");
    }

    private static SAXBuilder createSaxBuilder() {
        SAXBuilder sb = new SAXBuilder();
        // don't validate and don't load dtd
        sb.setValidation(false);
        sb.setFeature("http://xml.org/sax/features/validation", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return sb;
    }

    /**
     * Returns a "changed" comment element
     *
     * @param changedType Type of change ("Added", "Edited", etc)
     * @return Comment element
     */
    private static Comment getChangedComment(String changedType) {
        return new Comment(new StringBuilder().append(changedType).append(" by Artifactory update (").
                append(new Date().toString()).append(")").toString());
    }
}
