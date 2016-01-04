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

package org.artifactory.version.converter.v167;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Adds user lock configuration
 *
 * @author Michael Pasternak
 */
public class UserLockConfigConverter implements XmlConverter {

    private static final Logger log = LoggerFactory.getLogger(UserLockConfigConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting default 'user locking' conversion");
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        Element securityConfigElement = rootElement.getChild("security", namespace);
        if (securityConfigElement == null ||
                rootElement.getChild("userLockPolicy", namespace) == null) {
            addDefaultConfig(rootElement, namespace);
        }
        log.info("Finished default 'user locking' conversion");
    }

    private void addDefaultConfig(Element rootElement, Namespace namespace) {

        Element securityConfigElement = rootElement.getChild("security", namespace);
        if(securityConfigElement == null ||
                securityConfigElement.getChild("userLockPolicy", namespace) != null) {
            return;
        }

        Element userLockPolicyElement = securityConfigElement.getChild("userLockPolicy", namespace);
        if(userLockPolicyElement == null) {
            UserLockPolicy userLockPolicy = new UserLockPolicy();
            userLockPolicyElement = new Element("userLockPolicy", namespace);

            Namespace passwordConfigNs = userLockPolicyElement.getNamespace();
            ArrayList<Element> elements = Lists.newArrayList();
            elements.add(new Element("enabled", passwordConfigNs)
                    .setText(String.valueOf(userLockPolicy.isEnabled())));
            elements.add(new Element("loginAttempts", passwordConfigNs)
                    .setText(String.valueOf(userLockPolicy.getLoginAttempts())));

            userLockPolicyElement.addContent(userLockPolicyElement.getContentSize(), elements);
            securityConfigElement.addContent(securityConfigElement.getContentSize(), userLockPolicyElement);
        }
    }
}

