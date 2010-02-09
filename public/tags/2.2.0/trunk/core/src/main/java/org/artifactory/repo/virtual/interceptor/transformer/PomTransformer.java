/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.repo.virtual.interceptor.transformer;

import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.version.XmlConverterUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * POM transformer which removes all not needed tags from both the POM itself and the profile tags. Used to strip the
 * repository deceleration from the POM itself.
 *
 * @author Eli Givoni
 * @author Tomer Cohen
 */
public class PomTransformer {
    private static final Logger log = LoggerFactory.getLogger(PomTransformer.class);

    private final String pomAsString;
    private final PomCleanupPolicy pomCleanupPolicy;

    public PomTransformer(String pomAsString, PomCleanupPolicy pomCleanupPolicy) {
        if (pomAsString == null) {
            throw new IllegalArgumentException("Null pom content is not allowed");
        }
        this.pomCleanupPolicy = pomCleanupPolicy;
        this.pomAsString = pomAsString;
    }

    public String transform() {
        if (pomCleanupPolicy.equals(PomCleanupPolicy.nothing)) {
            return pomAsString;
        }
        Document pomDocument;
        try {
            //delete repositories and pluginsRepositories
            //Maven model does not preserve layout
            pomDocument = XmlConverterUtils.parse(pomAsString);
        } catch (Exception e) {
            log.warn("Failed to parse pom '{}': ", e.getMessage());
            return pomAsString;
        }
        Element pomRoot = pomDocument.getRootElement();
        Namespace namespace = pomRoot.getNamespace();
        pomRoot.removeChild("repositories", namespace);
        pomRoot.removeChild("pluginRepositories", namespace);
        boolean onlyActiveDefault = pomCleanupPolicy.equals(PomCleanupPolicy.discard_active_reference);

        //delete repositories and pluginsRepositories in profiles
        Element profilesElement = pomRoot.getChild("profiles", namespace);
        if (profilesElement != null) {
            List profiles = profilesElement.getChildren();
            for (Object profile : profiles) {
                Element profileElement = (Element) profile;
                if (onlyActiveDefault) {
                    boolean activeByDefault = false;
                    Element activationElement = profileElement.getChild("activation", namespace);
                    if (activationElement != null) {
                        Element activationByDefault = activationElement.getChild("activeByDefault", namespace);
                        if (activationByDefault != null) {
                            activeByDefault = Boolean.parseBoolean(activationByDefault.getText());
                        }
                    }
                    if (activeByDefault) {
                        deleteProfileRepositories(profileElement, namespace);
                    }
                } else {
                    deleteProfileRepositories(profileElement, namespace);
                }
            }
        }
        return XmlConverterUtils.outputString(pomDocument);
    }

    private void deleteProfileRepositories(Element profile, Namespace namespace) {
        profile.removeChild("repositories", namespace);
        profile.removeChild("pluginRepositories", namespace);
    }
}
