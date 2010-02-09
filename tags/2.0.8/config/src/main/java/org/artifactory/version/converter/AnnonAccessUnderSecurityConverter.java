package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Convert:
 * &lt;anonAccessEnabled&gt;XXX&lt;/anonAccessEnabled&gt;
 * to:
 * &lt;security&gt;
 *     &lt;anonAccessEnabled&gt;XXX&lt;/anonAccessEnabled&gt;
 *     .....
 * &lt;/security&gt;
 * </pre>
 * <p/>
 * anonAccessEnabled and security were not mandatory so need to check for nulls.
 * Was valid until version 1.3.0 of the schema.
 *
 * @author Yossi Shaul
 */
public class AnnonAccessUnderSecurityConverter implements XmlConverter {
    private static final Logger log =
            LoggerFactory.getLogger(AnnonAccessUnderSecurityConverter.class);

    public void convert(Document doc) {

        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element annonAccess = root.getChild("anonAccessEnabled", ns);
        if (annonAccess == null) {
            log.debug("Element anonAccessEnabled not found");
            return;
        }

        root.removeContent(annonAccess);

        Element security = root.getChild("security", ns);
        if (security == null) {
            security = new Element("security", ns);
            security.addContent(annonAccess);
            // serverName, anonAccessEnabled, fileUploadMaxSizeMb, dateFormat
            int location = findLastLocation(root,
                    "serverName", "anonAccessEnabled", "fileUploadMaxSizeMb", "dateFormat");
            root.addContent(location + 1, security);
        } else {
            security.addContent(0, annonAccess);
        }
    }

    private int findLastLocation(Element parent, String... elements) {
        for (int i = elements.length - 1; i >= 0; i--) {
            Element child = parent.getChild(elements[i], parent.getNamespace());
            if (child != null) {
                return parent.indexOf(child);
            }
        }
        return -1;
    }
}
