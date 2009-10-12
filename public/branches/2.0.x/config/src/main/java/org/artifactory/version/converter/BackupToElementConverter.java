package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Convers
 *    &lt;backupDir&gt;XX&lt;/backupDir&gt;
 *    &lt;backupCronExp&gt;YY&lt;/backupCronExp&gt;
 * to:
 *    &lt;backup&gt;
 *        &lt;dir&gt;XX&lt;/dir&gt;
 *        &lt;cronExp&gt;YY&lt;/cronExp&gt;
 *    &lt;/backup&gt;
 * </pre>
 *
 * Those backup elements were directly under the root in version 1.0.0 of the schema.
 * The new backup element should be placed before the localRepositories element.
 *
 * @author Yossi Shaul
 */
public class BackupToElementConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(BackupToElementConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        Element backupDir = root.getChild("backupDir", ns);
        if (backupDir != null) {
            root.removeContent(backupDir);
            backupDir.setName("dir");
            log.debug("Renamed 'backupDir' to 'dir'");
        }

        Element backupCron = root.getChild("backupCronExp", ns);
        if (backupCron != null) {
            root.removeContent(backupCron);
            backupCron.setName("cronExp");
            log.debug("Renamed 'backupCronExp' to 'cronExp'");
        }

        if (backupDir != null || backupCron != null) {
            // create the new <backup> element and place before the localRepositories
            Element backup = new Element("backup", ns);
            backup.addContent(backupDir);
            backup.addContent(backupCron);
            int localReposLocation = root.indexOf(root.getChild("localRepositories", ns));
            root.addContent(localReposLocation, backup);
        } else {
            log.debug("No backup elements found");
        }
    }
}
