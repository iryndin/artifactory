package org.artifactory.version.converter.v142;

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Eli Givoni
 */
public class RepoIncludeExcludePatternsConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(RepoIncludeExcludePatternsConverter.class);


    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element localReposElement = root.getChild("localRepositories", ns);
        List localRepos = localReposElement == null ? null : localReposElement.getChildren("localRepository", ns);

        Element remotelReposElement = root.getChild("remoteRepositories", ns);
        List remoteRepos = remotelReposElement == null ? null : remotelReposElement.getChildren("remoteRepository", ns);

        movePatternsElements(localRepos, ns);
        movePatternsElements(remoteRepos, ns);
    }

    private void movePatternsElements(List repos, Namespace ns) {
        if (repos != null) {
            for (Object repo : repos) {
                Element repoElement = (Element) repo;

                Element includesPattern = repoElement.getChild("includesPattern", ns);
                Element excludePattern = repoElement.getChild("excludesPattern", ns);

                setPatternsElements(repoElement, includesPattern, ns);
                setPatternsElements(repoElement, excludePattern, ns);
            }
        }
    }

    private void setPatternsElements(Element repoElement, Element patternElement, Namespace ns) {
        if (patternElement == null) {
            return;
        }
        repoElement.removeContent(patternElement);
        int location;
        Element lookForElement = repoElement.getChild("includesPattern", ns);
        if (lookForElement != null) {
            location = repoElement.indexOf(lookForElement);
            repoElement.addContent(location + 1, patternElement);
            return;
        }
        lookForElement = repoElement.getChild("type", ns);
        if (lookForElement != null) {
            location = repoElement.indexOf(lookForElement);
            repoElement.addContent(location + 1, patternElement);
            return;
        }

        lookForElement = repoElement.getChild("description", ns);
        if (lookForElement != null) {
            location = repoElement.indexOf(lookForElement);
            repoElement.addContent(location + 1, patternElement);
            return;
        }

        lookForElement = repoElement.getChild("key", ns);
        location = repoElement.indexOf(lookForElement);
        repoElement.addContent(location + 1, patternElement);
    }
}

