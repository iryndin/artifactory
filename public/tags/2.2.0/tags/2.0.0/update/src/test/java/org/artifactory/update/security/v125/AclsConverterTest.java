package org.artifactory.update.security.v125;

import org.artifactory.update.security.v130beta1.SimpleUserConverterTest;
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
public class AclsConverterTest extends SimpleUserConverterTest {
    private final static Logger log = LoggerFactory.getLogger(AclsConverterTest.class);

    @Test
    public void convertOutputOfSimpleUserConverter() throws Exception {
        String fileMetadata = "/security/v125/security.xml";
        Document doc = convertMetadata(fileMetadata, new AclsConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();

        assertNull(root.getChild("repoPaths"), "repoPaths element should be removed");

        Element aclsElement = root.getChild("acls");
        List acls = aclsElement.getChildren();
        assertEquals(acls.size(), 2, "Expecting two acls");

        Element acl1 = (Element) acls.get(0);
        assertEquals(acl1.getName(), "org.artifactory.security.RepoPathAcl", "ACL element not renamed");
        assertNull(acl1.getChildText("aclObjectIdentity"), "aclObjectIdentity should have been replaced");
        assertEquals(acl1.getChildText("identifier"), "ANY%3aANY", "Acl identifier mismatch");
        Element acesElement = acl1.getChild("aces");
        assertNotNull(acesElement, "aces element shouldn't be null");
        Element listElement = acesElement.getChild("list");
        assertNotNull(listElement, "List element should not be null");
        List aces = listElement.getChildren("org.artifactory.security.RepoPathAce");
        assertEquals(aces.size(), 1, "Expecting one ace");
        Element pathAce = (Element) aces.get(0);
        assertEquals(pathAce.getChildText("principal"), "anonymous", "Expected anonymous user");
        assertEquals(pathAce.getChildText("mask"), "1", "Expected mask of 1");

        Element acl2 = (Element) acls.get(1);
        assertEquals(acl2.getName(), "org.artifactory.security.RepoPathAcl", "ACL element not renamed");
        assertNull(acl2.getChildText("aclObjectIdentity"), "aclObjectIdentity should have been replaced");
        assertEquals(acl2.getChildText("identifier"), "libs-releases%3aorg.apache", "Acl identifier mismatch");
        Element aces2Element = acl2.getChild("aces");
        assertNotNull(aces2Element, "aces element shouldn't be null");
        listElement = aces2Element.getChild("list");
        assertNotNull(listElement, "List element should not be null");
        aces = listElement.getChildren("org.artifactory.security.RepoPathAce");
        assertEquals(aces.size(), 2, "Expecting one ace");
        pathAce = (Element) aces.get(0);
        assertEquals(pathAce.getChildText("principal"), "momo", "Unexpected user");
        assertEquals(pathAce.getChildText("mask"), "7", "Unexpected mask");
        pathAce = (Element) aces.get(1);
        assertEquals(pathAce.getChildText("principal"), "yossis", "Unexpected user");
        assertEquals(pathAce.getChildText("mask"), "6", "Unexpected mask");
    }
}