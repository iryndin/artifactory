package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <pre>
 * Converts:
 * &lt;snapshotVersionBehavior&gt;nonunique&lt;/snapshotVersionBehavior&gt;
 * Into:
 * &lt;snapshotVersionBehavior&gt;non-unique&lt;/snapshotVersionBehavior&gt;
 * </pre>
 *
 * The element snapshotVersionBehavior might appear under a localRepository element.
 * Was valid until version 1.1.0 of the schema.
 *
 * @author Yossi Shaul
 */
public class SnapshotNonUniqueValueConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(SnapshotNonUniqueValueConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        List localRepos = root.getChild("localRepositories", ns).getChildren();
        for (Object localRepo1 : localRepos) {
            Element localRepo = (Element) localRepo1;
            Element snapshotBehavior = localRepo.getChild("snapshotVersionBehavior", ns);
            if (snapshotBehavior != null && "nonunique".equals(snapshotBehavior.getText())) {
                log.debug("Changing value 'nonunique' to 'non-unique' for repo {}",
                        localRepo.getChildText("key", ns));
                snapshotBehavior.setText("non-unique");
            }
        }
    }
}