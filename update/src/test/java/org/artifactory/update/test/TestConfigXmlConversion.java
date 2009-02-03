package org.artifactory.update.test;

import org.apache.log4j.Logger;
import org.artifactory.common.ArtifactoryConstants;
import org.testng.annotations.Test;

/**
 * User: freds Date: May 28, 2008 Time: 9:39:32 PM
 */
public class TestConfigXmlConversion {
    private final static Logger LOGGER = Logger.getLogger(TestConfigXmlConversion.class);

    //TODO: [by yl] Fred = need to revive these tests!
    @Test
    public void testConvert100() throws Exception {
        ArtifactoryConstants.substituteRepoKeys.clear();
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rdp-releases", "third-party-releases");
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rdp-snapshots", "third-party-snapshots");
        ArtifactoryConstants.fillRepoKeySubstitute();
        /* CentralConfig cc = transform("/config/install/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        assertTrue(cc.isAnonAccessEnabled());
        ArtifactoryConstants.substituteRepoKeys.clear();
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rd-party", "third-party");
        ArtifactoryConstants.fillRepoKeySubstitute();
        cc = transform("/config/test/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        assertTrue(cc.isAnonAccessEnabled());*/
    }

    @Test
    public void testConvert110() throws Exception {
        /*CentralConfig cc = transform("/config/install/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        assertTrue(cc.isAnonAccessEnabled());
        cc = transform("/config/test/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        assertTrue(cc.isAnonAccessEnabled());*/
    }

    @Test
    public void testConvert120() throws Exception {
        /*CentralConfig cc = transform("/config/install/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        assertTrue(cc.isAnonAccessEnabled());
        cc = transform("/config/test/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        assertTrue(cc.isAnonAccessEnabled());*/
    }

    @Test
    public void testConvert130() throws Exception {
        /*CentralConfig cc = transform("/config/install/config.1.3.0.xml", ArtifactoryConfigVersion.OneThree);
        assertTrue(cc.isAnonAccessEnabled());
        cc = transform("/config/test/config.1.3.0.xml", ArtifactoryConfigVersion.OneThree);
        assertTrue(cc.isAnonAccessEnabled());*/
    }

    /*private CentralConfigServiceImpl transform(String textXml, ArtifactoryConfigVersion version)
            throws IOException, JAXBException, SAXException {
        InputStream is = getClass().getResourceAsStream(textXml);
        String originalXmlString = IOUtils.toString(is, "utf-8");
        String finalConfigXml = version.convert(originalXmlString);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Converted:\n" + originalXmlString + "\nto:\n" + finalConfigXml);
        }
        return getConfigValid(finalConfigXml);
    }

    private CentralConfigServiceImpl getConfigValid(String finalConfigXml)
            throws JAXBException, SAXException {
        JAXBContext context = JAXBContext.newInstance(CentralConfigServiceImpl.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL xsdUrl = getClass().getResource("/artifactory.xsd");
        Schema schema = sf.newSchema(xsdUrl);
        unmarshaller.setSchema(schema);
        return (CentralConfigServiceImpl) unmarshaller.unmarshal(new StringReader(finalConfigXml));
    }*/
}
