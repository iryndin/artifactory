package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <pre>
 * Convert
 * &lt;useSnapshotUniqueVersions&gt;true&lt;/useSnapshotUniqueVersions&gt;
 * to
 * &lt;snapshotVersionBehavior&gt;deployer&lt;/snapshotVersionBehavior&gt;
 * and
 * &lt;useSnapshotUniqueVersions&gt;false&lt;/useSnapshotUniqueVersions&gt;
 * to
 * &lt;snapshotVersionBehavior&gt;non-unique&lt;/snapshotVersionBehavior&gt;
 * </pre>
 * Rename the element "useSnapshotUniqueVersions" to "snapshotVersionBehavior" and change
 * the values "true" to "deployer" and "false" to "non-unique".
 *
 * The element might appear under the localRepo element.
 * Was valid only in version 1.0.0 of the schema.
 *
 * @author Yossi Shaul
 */
public class SnapshotUniqueVersionConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(SnapshotUniqueVersionConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        List localRepos = root.getChild("localRepositories", ns).getChildren();
        for (Object localRepo1 : localRepos) {
            Element localRepo = (Element) localRepo1;
            Element snapshotBehavior = localRepo.getChild("useSnapshotUniqueVersions", ns);
            if (snapshotBehavior != null) {
                // rename the element
                snapshotBehavior.setName("snapshotVersionBehavior");
                String repoKey = localRepo.getChildText("key", ns);
                log.debug("Renamed element 'useSnapshotUniqueVersions' to " +
                        "'snapshotVersionBehavior' for repo {}", repoKey);

                // change the element value
                if (snapshotBehavior.getText().equals("true")) {
                    log.debug("Changed value 'true' to 'deployer' for repo {}", repoKey);
                    snapshotBehavior.setText("deployer");
                } else if (snapshotBehavior.getText().equals("false")) {
                    log.debug("Changed value 'false' to 'non-unique' for repo {}", repoKey);
                    snapshotBehavior.setText("non-unique");
                }
            }
        }
    }
}
