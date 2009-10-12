package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renames the element "anonDownloadsAllowed" to "anonAccessEnabled".
 * This element was directly under the root element.
 * Was valid until version 1.2.0 of the schema.
 *
 * @author Yossi Shaul
 */
public class AnonAccessNameConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(AnonAccessNameConverter.class);

    private final String OLD_ANNON = "anonDownloadsAllowed";
    private final String NEW_ANNON = "anonAccessEnabled";

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Element oldAnnon = root.getChild(OLD_ANNON, root.getNamespace());
        if (oldAnnon != null) {
            oldAnnon.setName(NEW_ANNON);
            log.debug("Element {} found and converted", OLD_ANNON);
        } else {
            log.debug("Element {} not found", OLD_ANNON);
        }
    }
}
