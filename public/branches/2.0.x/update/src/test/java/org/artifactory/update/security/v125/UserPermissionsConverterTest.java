package org.artifactory.update.security.v125;

import org.artifactory.update.security.SecurityConverterTest;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests the UserPermissionsConverter.
 *
 * @author Yossi Shaul
 */
public class UserPermissionsConverterTest extends SecurityConverterTest {
    private final static Logger log = LoggerFactory.getLogger(UserPermissionsConverterTest.class);

    @Test
    public void convertOutputOfSimpleUserConverter() throws Exception {
        String fileMetadata = "/security/v125/security.xml";
        Document doc = convertMetadata(fileMetadata, new UserPermissionsConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();

        Element usersElement = root.getChild("users");
        List users = usersElement.getChildren();
        Element admin = (Element) users.get(0);
        assertEquals(admin.getChildText("username"), "admin");
        assertEquals(admin.getChildText("admin"), "true");

        Element user1 = (Element) users.get(1);
        assertNull(user1.getChild("admin"));

        Element user2 = (Element) users.get(2);
        assertNull(user2.getChildText("admin"));

    }
}
