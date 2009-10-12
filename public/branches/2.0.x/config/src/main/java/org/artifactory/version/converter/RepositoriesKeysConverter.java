package org.artifactory.version.converter;

import org.artifactory.common.ArtifactoryProperties;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts repositories key values. This converter is neccessary for repository keys starting with
 * numbers since starting with 1.1.0 the repo keys are xml ids. The keys to replace should pass as
 * system properties and filled in  ArtifactoryConstants.
 *
 * @author Yossi Shaul
 */
public class RepositoriesKeysConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(RepositoriesKeysConverter.class);

    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        // get all local repositories
        Element localReposWrapper = root.getChild("localRepositories", ns);
        List localRepos = localReposWrapper.getChildren();
        List repos = new ArrayList(localRepos);

        // and add all remote repositories if any
        Element remoteReposWrapper = root.getChild("remoteRepositories", ns);
        if (remoteReposWrapper != null) {
            List remoteRepos = remoteReposWrapper.getChildren();
            if (remoteRepos != null) {
                repos.addAll(remoteRepos);
            }
        }

        for (Object repo : repos) {
            Element localRepo = (Element) repo;
            Element keyElement = localRepo.getChild("key", ns);
            String key = keyElement.getText();
            Map<String, String> keys = ArtifactoryProperties.get().getSubstituteRepoKeys();
            if (keys.containsKey(key)) {
                String newKey = keys.get(key);
                log.debug("Changing repository key from {} to {}", key, newKey);
                keyElement.setText(newKey);
            }
        }
    }
}
