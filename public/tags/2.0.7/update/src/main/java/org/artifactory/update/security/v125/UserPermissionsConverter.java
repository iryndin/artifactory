package org.artifactory.update.security.v125;

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This converter will add the admin element for admin users.
 *
 * @author Yossi Shaul
 */
public class UserPermissionsConverter implements XmlConverter {
    private final static Logger log = LoggerFactory.getLogger(UserPermissionsConverter.class);

    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element root = doc.getRootElement();

        Element usersElement = root.getChild("users");
        if (usersElement == null) {
            log.warn("No users found");
            return;
        }

        List<Element> users = usersElement.getChildren("org.artifactory.security.SimpleUser");
        for (Element user : users) {
            Element authoritiesElement = user.getChild("authorities");
            if (authoritiesElement == null) {
                log.warn("No authorities found for {}", user.getChildText("username"));
                continue;
            }
            List<Element> authorities = authoritiesElement.getChildren("org.acegisecurity.GrantedAuthorityImpl");
            for (Element authority : authorities) {
                if ("ADMIN".equals(authority.getChildText("role"))) {
                    user.addContent(new Element("admin").setText("true"));
                    break;
                }
            }
        }
    }
}
