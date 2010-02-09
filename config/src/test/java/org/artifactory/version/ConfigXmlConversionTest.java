/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.version;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.io.IOUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.EncryptionPolicy;
import org.artifactory.descriptor.security.PasswordSettings;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.XmlConverter;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * @author freds
 * @author Yossi Shaul
 */
public class ConfigXmlConversionTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigXmlConversionTest.class);

    @Test
    public void convert100() throws Exception {
        ArtifactorySystemProperties.bind(new ArtifactorySystemProperties());
        System.setProperty(ConstantValues.substituteRepoKeys.getPropertyName() +
                "3rdp-releases", "third-party-releases");
        System.setProperty(ConstantValues.substituteRepoKeys.getPropertyName() +
                "3rdp-snapshots", "third-party-snapshots");
        ArtifactorySystemProperties.get().loadArtifactorySystemProperties(null, null);
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.0.0.xml", ArtifactoryConfigVersion.v100);
        assertNotNull(cc.getSecurity());
        assertTrue(cc.getSecurity().isAnonAccessEnabled(),
                "Annon access should be enabled by default");

        System.setProperty(ConstantValues.substituteRepoKeys.getPropertyName() +
                "3rd-party", "third-party");
        ArtifactorySystemProperties.get().loadArtifactorySystemProperties(null, null);
        cc = transform("/config/test/config.1.0.0.xml", ArtifactoryConfigVersion.v100);
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
        ArtifactorySystemProperties.unbind();
    }

    @Test
    public void convert100NoBackupCron() throws Exception {
        ArtifactorySystemProperties.bind(new ArtifactorySystemProperties());
        CentralConfigDescriptor cc =
                transform("/config/test/config.1.0.0_no-backup-cron.xml", ArtifactoryConfigVersion.v100);
        assertNotNull(cc.getBackups());
        assertTrue(cc.getBackups().isEmpty());
        ArtifactorySystemProperties.unbind();
    }

    @Test
    public void convert110() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.1.0.xml", ArtifactoryConfigVersion.v110);
        assertNotNull(cc.getSecurity());
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.1.0.xml", ArtifactoryConfigVersion.v110);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void testConvert120() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.2.0.xml", ArtifactoryConfigVersion.v120);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.2.0.xml", ArtifactoryConfigVersion.v120);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void convert130() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.0.xml", ArtifactoryConfigVersion.v130);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        cc = transform("/config/test/config.1.3.0.xml", ArtifactoryConfigVersion.v130);
        assertNotNull(cc.getSecurity());
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
    }

    @Test
    public void convert131() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.1.xml", ArtifactoryConfigVersion.v131);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
        assertNull(cc.getSecurity().getLdapSettings());

        cc = transform("/config/test/config.1.3.1.xml", ArtifactoryConfigVersion.v131);
        assertFalse(cc.getSecurity().isAnonAccessEnabled());
        assertNotNull(cc.getSecurity().getLdapSettings());
        assertEquals(cc.getSecurity().getLdapSettings().size(), 1);
    }

    @Test
    public void convert132Install() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/install/config.1.3.2.xml", ArtifactoryConfigVersion.v132);
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
                transform("/config/test/config.1.3.2.xml", ArtifactoryConfigVersion.v132);

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
    public void convert132WithNoSchemaLocation() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/test/config.1.3.2_no-location.xml", ArtifactoryConfigVersion.v132);
        assertTrue(cc.getSecurity().isAnonAccessEnabled());
        assertEquals(cc.getFileUploadMaxSizeMb(), 100);
    }

    @Test
    public void convert134Install() throws Exception {
        CentralConfigDescriptor cc = transform("/config/install/config.1.3.4.xml", ArtifactoryConfigVersion.v134);

        // PasswordSettings were added in 1.3.5 - no converter was needed as this is not
        // a required tag and we can use the default
        PasswordSettings passwordSettings = cc.getSecurity().getPasswordSettings();
        assertNotNull(passwordSettings, "Passwords settings should not be null");
        assertEquals(passwordSettings.getEncryptionPolicy(), EncryptionPolicy.SUPPORTED,
                "If the default was changed, a converter needs to do it!");
    }

    @Test
    public void convert135Install() throws Exception {
        CentralConfigDescriptor cc = transform("/config/install/config.1.3.5.xml", ArtifactoryConfigVersion.v135);

        // PropertySets were added in 1.3.6 - no converter was needed as this is not
        // a required tag and we can use the default
        List<PropertySet> propertySets = cc.getPropertySets();
        assertNotNull(propertySets, "Property sets should not be null.");
        assertTrue(propertySets.isEmpty(), "Property sets list should be empty by default.");

        // keypair (no conversion needed)
        VirtualRepoDescriptor virtualRepo = cc.getVirtualRepositoriesMap().values().iterator().next();
        assertNull(virtualRepo.getKeyPair(), "Keypair should be null");
    }

    @Test
    public void convert135ProxyWithDomain() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/test/config.1.3.5_proxy-with-domain.xml", ArtifactoryConfigVersion.v135);

        List<ProxyDescriptor> proxies = cc.getProxies();
        ProxyDescriptor proxy = proxies.get(0);
        if (!"nt-proxy".equals(proxy.getKey())) {
            proxy = proxies.get(1);
        }
        assertEquals(proxy.getNtHost(), InetAddress.getLocalHost().getHostName(), "Wrong hostname");
    }

    @Test
    public void convert140DefaultProxy() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/test/config.1.4.0_default-proxy.xml", ArtifactoryConfigVersion.v140);

        List<ProxyDescriptor> proxies = cc.getProxies();
        ProxyDescriptor proxy = proxies.get(0);
        assertTrue(proxy.isDefaultProxy(), "Proxy not default");
    }

    @Test
    public void convert141() throws Exception {
        CentralConfigDescriptor cc = transform("/config/install/config.1.4.1.xml", ArtifactoryConfigVersion.v141);
        assertNull(cc.getLocalRepositoriesMap().get("libs-releases-local").getExcludesPattern());
        assertEquals(cc.getRemoteRepositoriesMap().get("java.net.m2").getExcludesPattern(), "commons-*,org/apache/**");
        assertEquals(cc.getRemoteRepositoriesMap().get("java.net.m2").getIncludesPattern(), "**/*");
        assertEquals(cc.getRemoteRepositoriesMap().get("codehaus").getIncludesPattern(), "org/**,com/**,net/**");
        assertEquals(cc.getRemoteRepositoriesMap().get("codehaus").getExcludesPattern(),
                "org/apache/**,commons-*,org/artifactory/**,org/jfrog/**");
        assertEquals(cc.getVirtualRepositoriesMap().get("libs-releases").getIncludesPattern(), "**/*");
        assertNull(cc.getVirtualRepositoriesMap().get("libs-releases").getExcludesPattern());
    }

    @Test
    public void convert141Custom() throws Exception {
        CentralConfigDescriptor cc =
                transform("/config/test/config.1.4.1_with_type.xml", ArtifactoryConfigVersion.v141);

        assertEquals(cc.getLocalRepositoriesMap().get("libs-snapshots-local").getIncludesPattern(), "org/jfrog/**");
        assertNull(cc.getLocalRepositoriesMap().get("libs-snapshots-local").getExcludesPattern());
        assertEquals(cc.getRemoteRepositoriesMap().get("jfrog-libs").getExcludesPattern(), "org/apache/maven/**");
    }

    @Test
    public void configVersionSanityCheck() {

        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        Set<XmlConverter> allConversions = new HashSet<XmlConverter>();
        ArtifactoryConfigVersion configVersionTest = null;
        for (ArtifactoryConfigVersion configVersion : versions) {
            XmlConverter[] versionConversions = configVersion.getConverters();
            for (XmlConverter converter : versionConversions) {
                assertFalse(allConversions.contains(converter), "XML Converter element can only be used once!\n" +
                        "XML Converter " + converter + " is used in " + configVersion + " but was already used.");
                allConversions.add(converter);
            }
            configVersionTest = configVersion;
        }

        boolean configNotCurrent = configVersionTest != getCurrentVersion();
        assertFalse(configNotCurrent, "The last config version " + configVersionTest +
                " is not the current one " + getCurrentVersion());

        // The last should not have any conversion
        XmlConverter[] currentConversions = getCurrentVersion().getConverters();
        boolean noConversionsLeft = (currentConversions != null) && (currentConversions.length > 0);
        assertFalse(noConversionsLeft, "The last config version " + configVersionTest +
                " should have any conversions declared");
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

    private ArtifactoryConfigVersion getCurrentVersion() {
        ArtifactoryConfigVersion[] artifactoryConfigVersions = ArtifactoryConfigVersion.values();
        return artifactoryConfigVersions[artifactoryConfigVersions.length - 1];
    }
}
