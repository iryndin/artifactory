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

package org.artifactory.version.converter.v168;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.security.PasswordExpirationPolicy;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.descriptor.trashcan.TrashcanConfigDescriptor;
import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Adds password policy config section with default value where applicable
 *
 * @author Michael Pasternak
 */
public class PasswordPolicyConverter implements XmlConverter {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting user PasswordPolicyConverter conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        Element securityConfigElement = rootElement.getChild("security", namespace);
        if (securityConfigElement == null) {
            securityConfigElement = new Element("security", namespace);
        }

        Element passwordSettings = securityConfigElement.getChild("passwordSettings", namespace);
        if (passwordSettings == null) {
            return;
        }

        Element expirationPolicy = passwordSettings.getChild("expirationPolicy", passwordSettings.getNamespace());
        if(expirationPolicy == null) {
            expirationPolicy = new Element("expirationPolicy", passwordSettings.getNamespace());

            Namespace expirationPolicyNs = expirationPolicy.getNamespace();

            PasswordExpirationPolicy passwordExpirationPolicy = new PasswordExpirationPolicy();

            ArrayList<Element> elements = Lists.newArrayList();
            elements.add(new Element("enabled", expirationPolicyNs)
                    .setText(String.valueOf(passwordExpirationPolicy.isEnabled())));
            elements.add(new Element("expiresIn", expirationPolicyNs)
                    .setText(String.valueOf(passwordExpirationPolicy.getExpiresIn())));
            elements.add(new Element("notifyByEmail", expirationPolicyNs)
                    .setText(String.valueOf(passwordExpirationPolicy.isNotifyByEmail())));

            expirationPolicy.addContent(expirationPolicy.getContentSize(), elements);
            passwordSettings.addContent(passwordSettings.getContentSize(), expirationPolicy);
        }
        log.info("Finished PasswordPolicyConverter conversion");
    }
}
