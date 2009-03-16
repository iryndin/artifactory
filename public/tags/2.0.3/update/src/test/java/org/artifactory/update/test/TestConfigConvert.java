package org.artifactory.update.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.update.config.ArtifactoryConfigUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Test that no virtual repos appear in the excludes list
 *
 * @author Noam Tenne
 */
public class TestConfigConvert {
    @Test
    public void testConvert125u1() throws Exception {
        InputStream is = getClass().getResourceAsStream("/v125u1/artifactory.config.xml");
        File dirExport = new File(System.getProperty("java.io.tmpdir"), "testConvert125u1");
        FileUtils.deleteDirectory(dirExport);
        FileUtils.forceMkdir(dirExport);
        File confFile = new File(dirExport, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        FileOutputStream os = new FileOutputStream(confFile);
        IOUtils.copy(is, os);
        os.close();
        // Convert to latest version
        JaxbHelper.readConfig(confFile);
        // Convert local to virtual
        ArtifactoryConfigUpdate.migrateLocalRepoToVirtual(dirExport);
        CentralConfigDescriptorImpl descriptor = JaxbHelper.readConfig(confFile);
        Assert.assertTrue(
                descriptor.getBackups().get(0).getExcludedRepositories().get(0) instanceof RealRepoDescriptor);
    }
}
