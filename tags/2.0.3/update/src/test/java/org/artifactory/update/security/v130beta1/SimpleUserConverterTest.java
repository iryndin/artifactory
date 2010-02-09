package org.artifactory.update.security.v130beta1;

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
 * Tests the SimpleUserConverter.
 *
 * @author Yossi Shaul
 */
@Test
public class SimpleUserConverterTest extends SecurityConverterTest {
    private final static Logger log = LoggerFactory.getLogger(SimpleUserConverterTest.class);

    public void convertValidFile() throws Exception {
        String fileMetadata = "/security/v130beta1/security.xml";
        Document doc = convertMetadata(fileMetadata, new SimpleUserConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();
        Element usersElement = root.getChild("users");
        List users = usersElement.getChildren();
        assertEquals(users.size(), 4, "Expecting 4 users");
        Element user = (Element) users.get(0);
        assertEquals(user.getName(), "user", "User element name not changed");
        assertNull(user.getChild("authorities"), "Authorities should be removed");
        assertEquals(user.getChildText("username"), "admin", "Username mismatch");
        assertEquals(user.getChildText("admin"), "true", "Should be admin");

        user = (Element) users.get(1);
        assertEquals(user.getName(), "user", "User element name not changed");
        assertNull(user.getChild("authorities"), "Authorities should be removed");
        assertEquals(user.getChildText("username"), "anonymous", "Username mismatch");
        assertEquals(user.getChildText("admin"), "false", "Should not be admin");
    }
}
