/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.version.converter.v166;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Michael Pasternak
 */
public class SourceDeletedDetectionConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(SourceDeletedDetectionConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Converting repositories for SourceDeletedDetection support");

        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element localRepos = rootElement.getChild("localRepositories", namespace);
        if (localRepos != null) {
            convertLocalRepos(localRepos.getChildren());
        }

        Element remoteRepos = rootElement.getChild("remoteRepositories", namespace);
        if (remoteRepos != null) {
            convertRemoteRepos(remoteRepos.getChildren());
        }

        Element virtualRepos = rootElement.getChild("virtualRepositories", namespace);
        if (virtualRepos != null) {
            convertVirtualRepos(virtualRepos.getChildren());
        }

        log.info("Finished Converting repositories for SourceDeletedDetection support");
    }

    private void convertLocalRepos(List<Element> repos) {
        if (repos == null || repos.isEmpty()) {
            return;
        }

        convertRepositories(repos);
    }

    private void convertRemoteRepos(List<Element> repos) {
        if (repos == null || repos.isEmpty()) {
            return;
        }

        convertRepositories(repos);
    }

    private void convertVirtualRepos(List<Element> repos) {
        if (repos == null || repos.isEmpty()) {
            return;
        }

        convertRepositories(repos);
    }

    private void convertRepositories(List<Element> repos) {
        for (Element repo : repos) {
            Element contentSynchronisation = repo.getChild("contentSynchronisation", repo.getNamespace());
            if (contentSynchronisation != null &&
                    contentSynchronisation.getChild("source", contentSynchronisation.getNamespace()) == null) {
                addOriginAbsenceDetectionElement(contentSynchronisation);
            }
        }
    }

    private void addOriginAbsenceDetectionElement(Element contentSynchronisation) {
        Element source = new Element("source", contentSynchronisation.getNamespace());
        Element originAbsenceDetection = new Element("originAbsenceDetection", source.getNamespace());
        originAbsenceDetection.addContent("false");
        source.addContent(source.getContentSize(), originAbsenceDetection);
        contentSynchronisation.addContent(contentSynchronisation.getContentSize(), source);
    }
}
