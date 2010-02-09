package org.artifactory.update.security.v130beta1;

import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests conversion done by RepoPathAclConverter.
 *
 * @author Yossi Shaul
 */
public class RepoPathAclConverterTest extends SimpleUserConverterTest {
    private final static Logger log = LoggerFactory.getLogger(RepoPathAclConverterTest.class);

    @Test
    public void convertOutputOfSimpleUserConverter() throws Exception {
        String fileMetadata = "/security/v130beta1/security.xml";
        Document doc = convertMetadata(fileMetadata, new RepoPathAclConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();

        assertNull(root.getChild("repoPaths"), "repoPaths element should be removed");

        Element aclssElement = root.getChild("acls");

        List acls = aclssElement.getChildren();
        assertEquals(acls.size(), 2, "Expecting two acls");

        // first acl
        Element acl1 = (Element) acls.get(0);
        assertEquals(acl1.getName(), "acl", "ACL element name not changed");
        assertNull(acl1.getChild("list"), "List should have been removed");

        assertEquals(acl1.getChildText("updatedBy"), "unknown");

        Element permTarget1 = acl1.getChild("permissionTarget");
        assertNotNull(permTarget1, "Permission target not generated");
        assertEquals(permTarget1.getChildText("name"), "Anything", "Permission target name mismatch");
        assertEquals(permTarget1.getChildText("repoKey"), "ANY", "Permission target repo key mismatch");
        Element acesElement1 = acl1.getChild("aces");
        assertNotNull(acesElement1, "Aces should not be null");

        List aces1 = acesElement1.getChildren("ace");
        assertEquals(aces1.size(), 1, "Expecting only 1 ace");
        Element ace1 = (Element) aces1.get(0);
        assertEquals(ace1.getChildText("principal"), "anonymous");
        assertEquals(ace1.getChildText("mask"), "1");
        assertEquals(ace1.getChildText("group"), "false");


        // second acl
        Element acl2 = (Element) acls.get(1);
        assertEquals(acl2.getName(), "acl", "ACL element name not changed");
        assertNull(acl2.getChild("list"), "List should have been removed");

        assertEquals(acl2.getChildText("updatedBy"), "admin");

        Element permTarget = acl2.getChild("permissionTarget");
        assertNotNull(permTarget, "Permission target not generated");
        assertEquals(permTarget.getChildText("name"), "libs-releases-local:org.art", "Permission target name mismatch");
        assertEquals(permTarget.getChildText("repoKey"), "libs-releases-local", "Permission target repo key mismatch");
        Element acesElement = acl2.getChild("aces");
        assertNotNull(acesElement, "Aces should not be null");

        List aces = acesElement.getChildren("ace");
        assertEquals(aces.size(), 2, "Expecting only 1 ace");
        Element ace = (Element) aces.get(1);
        assertEquals(ace.getChildText("principal"), "yossis");
        assertEquals(ace.getChildText("mask"), "11");   // with delete permissions
        assertEquals(ace.getChildText("group"), "false");
    }
}
