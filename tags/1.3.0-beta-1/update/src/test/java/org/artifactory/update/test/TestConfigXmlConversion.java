package org.artifactory.update.test;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.StringReader;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.artifactory.update.ArtifactoryConfigVersion;
import org.artifactory.config.CentralConfig;
import org.artifactory.ArtifactoryConstants;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;

/**
 * User: freds
 * Date: May 28, 2008
 * Time: 9:39:32 PM
 */
public class TestConfigXmlConversion extends TestCase {

    public void testConvert100() throws Exception {
        ArtifactoryConstants.substituteRepoKeys.clear();
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rdp-releases", "third-party-releases");
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rdp-snapshots", "third-party-snapshots");
        ArtifactoryConstants.fillRepoKeySubstitute();
        CentralConfig cc = transform("/config/install/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        assert(cc.isAnonAccessEnabled());
        ArtifactoryConstants.substituteRepoKeys.clear();
        System.setProperty(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST +
                "3rd-party", "third-party");
        ArtifactoryConstants.fillRepoKeySubstitute();
        cc = transform("/config/test/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        assert(cc.isAnonAccessEnabled());
    }

    public void testConvert110() throws Exception {
        CentralConfig cc = transform("/config/install/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        assert(cc.isAnonAccessEnabled());
        cc = transform("/config/test/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        assert(cc.isAnonAccessEnabled());
    }

    public void testConvert120() throws Exception {
        CentralConfig cc = transform("/config/install/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        assert(cc.isAnonAccessEnabled());
        cc = transform("/config/test/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        assert(cc.isAnonAccessEnabled());
    }

    private CentralConfig transform(String textXml, ArtifactoryConfigVersion version) throws IOException, JAXBException, SAXException {
        InputStream is = getClass().getResourceAsStream(textXml);
        String config120XmlString = IOUtils.toString(is, "UTF-8");
        String finalConfigXml = version.convert(config120XmlString);
        return getConfigValid(finalConfigXml);
    }

    private CentralConfig getConfigValid(String finalConfigXml) throws JAXBException, SAXException {
        JAXBContext context = JAXBContext.newInstance(CentralConfig.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL xsdUrl = getClass().getResource("/artifactory.xsd");
        Schema schema = sf.newSchema(xsdUrl);
        unmarshaller.setSchema(schema);
        return (CentralConfig) unmarshaller.unmarshal(new StringReader(finalConfigXml));
    }
}
