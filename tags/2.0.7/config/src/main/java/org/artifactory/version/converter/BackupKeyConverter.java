package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Add a key to the backup elements and remove backups without cronExp.
 * This change is from schema 1.3.2 to 1.3.3.
 *
 * @author Yossi Shaul
 */
public class BackupKeyConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(BackupKeyConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        Element backupsElement = root.getChild("backups", ns);
        List backups = null;
        if (backupsElement != null) {
            backups = backupsElement.getChildren("backup", ns);
            int generatedKeyIndex = 1;
            Iterator iterator = backups.iterator();
            while(iterator.hasNext()) {
                Element backup = (Element) iterator.next();
                Element cronExp = backup.getChild("cronExp", ns);
                if (cronExp == null) {
                    log.debug("Removing a backup without cron expression");
                    iterator.remove();
                    //backupsElement.removeContent(backup);
                } else {
                    // generate backup unique key and add to the backup element
                    String key = "backup" + generatedKeyIndex++;
                    Element keyElement = new Element("key", ns);
                    keyElement.setText(key);
                    backup.addContent(0, keyElement);
                    log.debug("Generated key '{}' for backup element", key);
                }
            }
        }

        if (backupsElement == null || backups.isEmpty()) {
            log.debug("No backups found");
        }

    }
}
