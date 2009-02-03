package org.artifactory.version;

import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.io.IOUtils;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.common.ConstantsValue;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

/**
 * @author freds
 * @author Yossi Shaul
 */
public class ConfigXmlConversionTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigXmlConversionTest.class);

    @BeforeClass
    public void setLevel() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        //lc.getLogger(ConfigXmlConversionTest.class).setLevel(Level.DEBUG);
        //lc.getLogger(ConverterUtils.class).setLevel(Level.DEBUG);
    }

    @Test
    public void convert100() throws Exception {
        System.setProperty(ConstantsValue.substituteRepoKeys.getPropertyName() +
                "3rdp-releases", "third-party-releases");
        System.setProperty(ConstantsValue.substituteRepoKeys.getPropertyName() +
                "3rdp-snapshots", "third-party-snapshots");
        ArtifactoryProperties.get().loadArtifactorySystemProperties(null, null);
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        assertNotNull(cc.getSecurity());
        assertTrue(cc.getSecurity().isAnonAccessEnabled(),
                "Annon access should be enabled by default");

        System.setProperty(ConstantsValue.substituteRepoKeys.getPropertyName() +
                "3rd-party", "third-party");
        ArtifactoryProperties.get().loadArtifactorySystemProperties(null, null);
        cc = transform("/config/test/config.1.0.0.xml", ArtifactoryConfigVersion.OneZero);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        OrderedMap<String, LocalRepoDescriptor> localRepos = cc.getLocalRepositoriesMap();

        LocalRepoDescriptor frogReleases = localRepos.get("frog-releases");
        assertEquals(frogReleases.getSnapshotVersionBehavior(),
                SnapshotVersionBehavior.DEPLOYER,
                "Should have been converted from 'true' to 'deployer'");

        LocalRepoDescriptor frogSnapshots = localRepos.get("frog-snapshots");
        assertEquals(frogSnapshots.getSnapshotVersionBehavior(),
                SnapshotVersionBehavior.NONUNIQUE,
                "Should have been converted from 'false' to 'non-unique'");

        LocalRepoDescriptor pluginsReleases = localRepos.get("plugins-releases");
        assertEquals(pluginsReleases.getSnapshotVersionBehavior(),
                SnapshotVersionBehavior.NONUNIQUE,
                "Should have kept the default");
    }

    @Test
    public void convert110() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        assertNotNull(cc.getSecurity());
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.1.0.xml", ArtifactoryConfigVersion.OneOne);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void testConvert120() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.2.0.xml", ArtifactoryConfigVersion.OneTwo);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void convert130() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.0.xml", ArtifactoryConfigVersion.OneThree);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.3.0.xml", ArtifactoryConfigVersion.OneThree);
        assertNotNull(cc.getSecurity());
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void convert131() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.1.xml", ArtifactoryConfigVersion.OneThreeOne);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
        assertNull(cc.getSecurity().getLdapSettings());

        cc = transform("/config/test/config.1.3.1.xml", ArtifactoryConfigVersion.OneThreeOne);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        assertNotNull(cc.getSecurity().getLdapSettings());
        assertEquals(cc.getSecurity().getLdapSettings().size(), 1);
    }

    @Test
    public void convert132Install() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.2.xml", ArtifactoryConfigVersion.OneThreeTwo);
        List<BackupDescriptor> backups = cc.getBackups();
        assertEquals(backups.size(), 1);

        // check backups conversion
        BackupDescriptor backup = backups.get(0);
        assertEquals(backup.getKey(), "backup1", "Unexpected backup key generated");
        assertTrue(backup.isEnabled(), "All existing backups should be enabled");
    }

    @Test
    public void convert132Custom() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/test/config.1.3.2.xml", ArtifactoryConfigVersion.OneThreeTwo);

        // check backups conversion
        List<BackupDescriptor> backups = cc.getBackups();
        assertEquals(backups.size(), 1, "Should have removed the second backup (no cronExp)");
        BackupDescriptor backup = backups.get(0);
        assertEquals(backup.getKey(), "backup1", "Unexpected backup key generated");
        assertTrue(backup.isEnabled(), "All existing backups should be enabled");

        // check ldap settings conversion
        List<LdapSetting> ldaps = cc.getSecurity().getLdapSettings();
        assertNotNull(ldaps);
        assertEquals(ldaps.size(), 3);
        LdapSetting ldap1 = ldaps.get(0);
        assertEquals(ldap1.getKey(), "ldap1");
        assertEquals(ldap1.getLdapUrl(), "ldap://mydomain:389/dc=jfrog,dc=org");
        assertEquals(ldap1.getUserDnPattern(), "uid={0}, ou=People");
        assertNull(ldap1.getSearch());

        LdapSetting ldap3 = ldaps.get(2);
        assertEquals(ldap3.getKey(), "ldap3");
        assertEquals(ldap3.getLdapUrl(), "ldap://mydomain:389/dc=jfrog,dc=org");
        assertEquals(ldap3.getLdapUrl(), "ldap://mydomain:389/dc=jfrog,dc=org");
        assertNull(ldap3.getUserDnPattern());
        SearchPattern search = ldap3.getSearch();
        assertNotNull(search);
        assertEquals(search.getSearchFilter(), "uid");
        assertEquals(search.getSearchBase(), "ou=Mice");
        assertFalse(search.isSearchSubTree());
        assertEquals(search.getManagerDn(), "koko");
        assertEquals(search.getManagerPassword(), "loko");
    }

    @Test
    public void convertWithNoSchemaLocation() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/test/noLocation.1.3.2.xml", ArtifactoryConfigVersion.OneThreeTwo);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
        assertEquals(cc.getFileUploadMaxSizeMb(), 100);
    }

    private CentralConfigDescriptor transform(String textXml, ArtifactoryConfigVersion version)
            throws Exception {
        InputStream is = getClass().getResourceAsStream(textXml);
        String originalXmlString = IOUtils.toString(is, "utf-8");
        ArtifactoryConfigVersion foundConfigVersion =
                ArtifactoryConfigVersion.getConfigVersion(originalXmlString);
        assertEquals(version, foundConfigVersion);
        String finalConfigXml = version.convert(originalXmlString);
        log.debug("Converted:\n{}\nto:\n{}", originalXmlString, finalConfigXml);
        return getConfigValid(finalConfigXml);
    }

    private CentralConfigDescriptor getConfigValid(String configXml) throws Exception {
        JAXBContext context = JAXBContext.newInstance(CentralConfigDescriptorImpl.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL xsdUrl = getClass().getResource("/artifactory.xsd");
        Schema schema = sf.newSchema(xsdUrl);
        unmarshaller.setSchema(schema);
        return (CentralConfigDescriptor) unmarshaller.unmarshal(new StringReader(configXml));
    }
}
