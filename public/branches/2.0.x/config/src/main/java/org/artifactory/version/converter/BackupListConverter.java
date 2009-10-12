package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * <pre>
 * Converts:
 *   &lt;backup&gt;...&lt;/backup&gt;
 * into:
 *   &lt;backups&gt;
 *     &lt;backup&gt;...&lt;/backup&gt;
 *   &lt;/backups&gt;
 * </pre>
 * The backup element was directly under the root element.
 * Was valid until version 1.3.0 of the schema.
 *
 * @author Yossi Shaul
 */
public class BackupListConverter implements XmlConverter {

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        Element backup = root.getChild("backup", ns);
        if (backup != null) {
            int location = root.indexOf(backup);
            root.removeContent(location);
            Element backups = new Element("backups", ns);
            backups.addContent(backup);
            root.addContent(location, backups);
        }
    }
}
