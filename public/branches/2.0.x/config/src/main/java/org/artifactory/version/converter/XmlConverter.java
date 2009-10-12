package org.artifactory.version.converter;

import org.jdom.Document;

/**
 * An XmlConverter converts one xml to another one so it will adhere to a new schema.
 *
 * @author Yossi Shaul
 */
public interface XmlConverter {
    void convert(Document doc);
}
