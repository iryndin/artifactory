package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schema version 1.3.1 removed the authenticationMethod and searchAuthPasswordAttributeName
 * from the ldap settings element.
 *
 * @author Yossi Shaul
 */
public class LdapSettings130Converter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(LdapSettings130Converter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        if (security == null) {
            log.debug("no security settings");
            return;
        }

        Element ldap = security.getChild("ldapSettings", ns);
        if (ldap == null) {
            log.debug("no ldap settings");
            return;
        }

        ldap.removeChild("authenticationMethod", ns);
        ldap.removeChild("searchAuthPasswordAttributeName", ns);
    }
}
