/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.version.converter.v169;

import org.artifactory.descriptor.security.PasswordExpirationPolicy;
import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Change the password expiration config field from expiresIn to passwordMaxAge
 *
 * @author Shay Yaakov
 */
public class PasswordMaxAgeConverter implements XmlConverter {

    private static final Logger log = LoggerFactory.getLogger(PasswordMaxAgeConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting user PasswordMaxAgeConverter conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element securityConfigElement = rootElement.getChild("security", namespace);
        if (securityConfigElement != null) {
            Element passwordSettings = securityConfigElement.getChild("passwordSettings", namespace);
            if (passwordSettings != null) {
                Element expirationPolicy = passwordSettings.getChild("expirationPolicy", namespace);
                if (expirationPolicy != null) {
                    String passwordMaxAge = String.valueOf(new PasswordExpirationPolicy().getPasswordMaxAge());
                    if (expirationPolicy.getChild("expiresIn", namespace) != null) {
                        passwordMaxAge = expirationPolicy.getChildText("expiresIn", namespace);
                        expirationPolicy.removeChild("expiresIn", namespace);
                    }
                    if (expirationPolicy.getChild("passwordMaxAge", namespace) == null) {
                        expirationPolicy.addContent(3, new Element("passwordMaxAge", namespace).setText(passwordMaxAge));
                    }
                }
            }
        }
        log.info("Finished PasswordMaxAgeConverter conversion");
    }
}
