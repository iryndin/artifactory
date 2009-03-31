package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Converts:
 *   &lt;ldapSettings&gt;
 *       ...
 *       &lt;userDnPattern&gt;...&lt;/userDnPattern&gt;
 *   &lt;/ldapSettings&gt;
 * into:
 *  &lt;authenticationPatterns&gt;&lt;authenticationPattern&gt;
 *      &lt;userDnPattern&gt;...&lt;/userDnPattern&gt;
 *  &lt;/authenticationPattern&gt;&lt;/authenticationPatterns&gt;
 * </pre>
 *
 * Was valid until version 1.3.1 of the schema.
 *
 * @author Yossi Shaul
 */
public class LdapAuthenticationPatternsConverter implements XmlConverter {
    private final static Logger log =
            LoggerFactory.getLogger(LdapAuthenticationPatternsConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element security = root.getChild("security", ns);
        if (security != null) {
            Element ldapSettings = security.getChild("ldapSettings", ns);
            if (ldapSettings != null) {
                Element userDn = ldapSettings.getChild("userDnPattern", ns);
                if (userDn != null) {
                    log.debug("Moving userDnPattern under authenticationPatterns");
                    int location = ldapSettings.indexOf(userDn);
                    ldapSettings.removeContent(userDn);
                    Element authPatterns = new Element("authenticationPatterns", ns);
                    Element authPattern = new Element("authenticationPattern", ns);
                    authPattern.addContent(userDn);
                    authPatterns.addContent(authPattern);
                    ldapSettings.addContent(location, authPatterns);
                }
            } else {
                log.debug("No ldap settings found");
            }
        }
    }
}
