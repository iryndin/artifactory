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

package org.artifactory.descriptor.reader;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.io.FileUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.jaxb.JaxbHelper;
import org.artifactory.test.SystemPropertiesBoundTest;
import org.artifactory.util.ResourceUtils;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test the {@link CentralConfigReader}
 *
 * @author Tomer Cohen
 */
@Test
public class CentralConfigReaderTest extends SystemPropertiesBoundTest {


    @AfterMethod
    public void clearArtifactoryProperties() {
        // clear any property that might have been set in a test
        for (ConstantValues constantsValue : ConstantValues.values()) {
            System.clearProperty(constantsValue.getPropertyName());
        }
    }


    public void readV6Config() throws Exception {
        File oldConfigFile = ResourceUtils.getResourceAsFile("/config/install/config.1.4.1.xml");
        CentralConfigDescriptor newConfig =
                new CentralConfigReader().read(oldConfigFile);
        OrderedMap<String, RemoteRepoDescriptor> descriptorOrderedMap = newConfig.getRemoteRepositoriesMap();
        Assert.assertEquals(descriptorOrderedMap.size(), 16, "Should contain 16 remote repository");
    }

    @SuppressWarnings("unchecked")
    public void readAllConfigFiles() throws Exception {
        System.setProperty(ConstantValues.substituteRepoKeys.getPropertyName() +
                "3rdp-releases", "third-party-releases");
        System.setProperty(ConstantValues.substituteRepoKeys.getPropertyName() +
                "3rdp-snapshots", "third-party-snapshots");
        ArtifactorySystemProperties.get().loadArtifactorySystemProperties(null, null);
        System.setProperty(ConstantValues.substituteRepoKeys.getPropertyName() +
                "3rd-party", "third-party");
        ArtifactorySystemProperties.get().loadArtifactorySystemProperties(null, null);
        File backupDirs = ResourceUtils.getResourceAsFile("/config");
        Collection<File> oldArtifactoryConfigs = FileUtils.listFiles(backupDirs, new String[]{"xml"}, true);
        assertTrue(oldArtifactoryConfigs.size() > 10, "Where are all my test files??");
        CentralConfigReader centralConfigReader = new CentralConfigReader();
        for (Object oldArtifactoryConfig : oldArtifactoryConfigs) {
            File file = (File) oldArtifactoryConfig;
            CentralConfigDescriptor newConfig = centralConfigReader.read(file);
            ArtifactoryConfigVersion configVersion =
                    ArtifactoryConfigVersion.getConfigVersion(JaxbHelper.toXml(newConfig));
            assertNotNull(configVersion, "Null value returned from security reader for file " + file.getAbsolutePath());
            assertTrue(configVersion.isCurrent(), "Artifactory config version is not up to date");
        }
        System.clearProperty(ConstantValues.substituteRepoKeys.getPropertyName() + "3rdp-releases");
        System.clearProperty(ConstantValues.substituteRepoKeys.getPropertyName() + "3rdp-snapshots");
        System.clearProperty(ConstantValues.substituteRepoKeys.getPropertyName() + "3rd-party");
    }
}
