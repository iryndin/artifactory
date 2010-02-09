package org.artifactory.update.test;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

/**
 * User: freds Date: May 28, 2008 Time: 9:39:32 PM
 */
public class TestConfigXmlConversion {
    private final static Logger LOGGER = Logger.getLogger(TestConfigXmlConversion.class);

    @BeforeClass
    public void setLevel() {
        //LOGGER.setLevel(Level.DEBUG);
    }

    @Test
    public void testConvert100() throws Exception {
        ArtifactoryConstants.substituteRepoKeys.clear();
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rdp-releases", "third-party-releases");
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rdp-snapshots", "third-party-snapshots");
        ArtifactoryConstants.fillRepoKeySubstitute();
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        Assert.assertNull(cc.getSecurity());
        ArtifactoryConstants.substituteRepoKeys.clear();
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rd-party", "third-party");
        ArtifactoryConstants.fillRepoKeySubstitute();
        cc = transform("/config/test/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        Assert.assertFalse(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void testConvert110() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        Assert.assertNull(cc.getSecurity());
        cc = transform("/config/test/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        Assert.assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void testConvert120() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        Assert.assertFalse(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        Assert.assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void testConvert130() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.0.xml", ArtifactoryConfigVersion.OneThree);
        Assert.assertFalse(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.3.0.xml", ArtifactoryConfigVersion.OneThree);
        Assert.assertNull(cc.getSecurity());
    }

    @Test
    public void testConvert131() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.1.xml", ArtifactoryConfigVersion.OneThreeOne);
        Assert.assertTrue(cc.getSecurity().isAnonAccessEnabled());
        Assert.assertNull(cc.getSecurity().getLdapSettings());
        cc = transform("/config/test/config.1.3.1.xml", ArtifactoryConfigVersion.OneThreeOne);
        Assert.assertFalse(cc.getSecurity().isAnonAccessEnabled());
        Assert.assertNotNull(cc.getSecurity().getLdapSettings());
    }

    private CentralConfigDescriptor transform(String textXml, ArtifactoryConfigVersion version)
            throws IOException, JAXBException, SAXException {
        InputStream is = getClass().getResourceAsStream(textXml);
        String originalXmlString = IOUtils.toString(is, "utf-8");
        ArtifactoryConfigVersion foundConfigVersion =
                ArtifactoryConfigVersion.getConfigVersion(originalXmlString);
        Assert.assertEquals(version, foundConfigVersion);
        String finalConfigXml = version.convert(originalXmlString);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Converted:\n" + originalXmlString + "\nto:\n" + finalConfigXml);
        }
        return getConfigValid(finalConfigXml);
    }

    private CentralConfigDescriptor getConfigValid(String finalConfigXml)
            throws JAXBException, SAXException {
        JAXBContext context = JAXBContext.newInstance(CentralConfigDescriptor.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL xsdUrl = getClass().getResource("/artifactory.xsd");
        Schema schema = sf.newSchema(xsdUrl);
        unmarshaller.setSchema(schema);
        return (CentralConfigDescriptor) unmarshaller.unmarshal(new StringReader(finalConfigXml));
    }
}
