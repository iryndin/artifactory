/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.jcr.version.v150;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.version.v150.xml.RepoXmlConverterTest;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ResourceUtils;
import org.artifactory.util.XmlUtils;
import org.jdom.Document;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

import static org.testng.Assert.assertTrue;

/**
 * Tests the behavior of the repo config converter
 *
 * @author Noam Y. Tenne
 */
public class RepoConfigConverterTest {

    private static final Logger log = LoggerFactory.getLogger(RepoConfigConverterTest.class);

    private Set<File> repoDirs = Sets.newHashSet();
    private ArtifactoryHome artifactoryHome;

    @AfterMethod
    public void tearDown() {
        ArtifactoryHome.unbind();
    }

    /**
     * Tests the config file location and normal conversion process
     */
    @Test
    public void testNormalConvert() throws Exception {
        setup(true, true);

        //Scan and convert
        RepoConfigConverterV150 repoConfigConverter = new RepoConfigConverterV150();
        repoConfigConverter.convert(artifactoryHome);

        //Validate that all planted repo.xml files were converted properly
        for (File repoDir : repoDirs) {
            InputStream is = null;
            try {
                is = new FileInputStream(new File(repoDir, ArtifactoryHome.ARTIFACTORY_JCR_FILE));
                Document doc = XmlUtils.parse(is);
                RepoXmlConverterTest xmlConverterTest = new RepoXmlConverterTest();
                xmlConverterTest.assertConvertedDoc(doc);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    /**
     * Tests the conversion of a default repo.xml with an unrecognized datastore
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void testConvertInvalidDefaultDatastore() throws Exception {
        setup(false, true);

        //Scan and convert
        RepoConfigConverterV150 repoConfigConverter = new RepoConfigConverterV150();
        repoConfigConverter.convert(artifactoryHome);

        //Validate that all planted repo.xml files were converted properly
        for (File repoDir : repoDirs) {
            InputStream is = null;
            try {
                is = new FileInputStream(new File(repoDir, ArtifactoryHome.ARTIFACTORY_JCR_FILE));
                Document doc = XmlUtils.parse(is);
                RepoXmlConverterTest xmlConverterTest = new RepoXmlConverterTest();
                xmlConverterTest.assertConvertedDoc(doc);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    /**
     * Tests the conversion of a default and optional repo.xml with an unrecognized datastore
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void testConvertInvalidDefaultAndOptionalDatastore() throws Exception {
        setup(false, false);

        //Scan and convert
        RepoConfigConverterV150 repoConfigConverter = new RepoConfigConverterV150();
        repoConfigConverter.convert(artifactoryHome);

        //Validate that all planted repo.xml files were converted properly
        for (File repoDir : repoDirs) {
            InputStream is = null;
            try {
                is = new FileInputStream(new File(repoDir, ArtifactoryHome.ARTIFACTORY_JCR_FILE));
                Document doc = XmlUtils.parse(is);
                RepoXmlConverterTest xmlConverterTest = new RepoXmlConverterTest();
                xmlConverterTest.assertConvertedDoc(doc);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    /**
     * Tests the conversion of an optional repo.xml with an unrecognized datastore
     */
    @Test
    public void testConvertInvalidOptionalDatastore() throws Exception {
        setup(true, false);

        //Scan and convert
        RepoConfigConverterV150 repoConfigConverter = new RepoConfigConverterV150();
        repoConfigConverter.convert(artifactoryHome);

        //Validate that all planted repo.xml files were converted properly
        for (File repoDir : repoDirs) {
            InputStream is = null;
            try {
                is = new FileInputStream(new File(repoDir, ArtifactoryHome.ARTIFACTORY_JCR_FILE));
                Document doc = XmlUtils.parse(is);
                RepoXmlConverterTest xmlConverterTest = new RepoXmlConverterTest();
                xmlConverterTest.assertConvertedDoc(doc);
            } catch (AssertionError assertionError) {
                log.info("Received expected assertion error: " + assertionError.getMessage());
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    /**
     * Setup the test environment
     *
     * @param validDefaultDatastore  True if should create a default repo.xml with a valid datastore, false for invalid
     * @param validOptionalDatastore True if should create a optional repo.xml with a valid datastore, false for
     *                               invalid
     * @throws IOException
     */
    private void setup(boolean validDefaultDatastore, boolean validOptionalDatastore) throws IOException {
        //Create env
        File homeDir = new File("target/test-output/repo-config-converter-test");
        File etcDir = new File(homeDir, "etc");
        FileUtils.deleteDirectory(homeDir);
        assertTrue(etcDir.mkdirs(), "Artifactory temp home should have been created.");

        /**
         * Configure in a.s.p a repo.xml file which is located in the home, so that we know it was found through a.s.p
         * and not through the scanning of the etc dir
         */
        InputStream propInStream = null;
        OutputStream propOutStream = null;
        Properties properties = new Properties();
        try {
            propInStream = ResourceUtils.getResource("/org/artifactory/jcr/version/v150/artifactory.system.properties");
            properties.load(propInStream);
            properties.setProperty(ConstantValues.jcrConfigDir.getPropertyName(), homeDir.toURI().getPath());
            propOutStream = new FileOutputStream(new File(etcDir, ArtifactoryHome.ARTIFACTORY_SYSTEM_PROPERTIES_FILE));
            properties.store(propOutStream, "mooooo");
        } finally {
            IOUtils.closeQuietly(propInStream);
            IOUtils.closeQuietly(propOutStream);
        }
        artifactoryHome = new ArtifactoryHome(homeDir);
        ArtifactoryHome.bind(artifactoryHome);

        File originalConfig = ResourceUtils.getResourceAsFile(validOptionalDatastore ?
                "/org/artifactory/jcr/version/v150/xml/repo.xml" :
                "/org/artifactory/jcr/version/v150/xml/repo-custom-ds.xml");
        File mysqlConfig = ResourceUtils.getResourceAsFile(validDefaultDatastore ?
                "/org/artifactory/jcr/version/v150/xml/mysql/repo.xml" :
                "/org/artifactory/jcr/version/v150/xml/mysql/repo-custom-ds.xml");
        assertTrue(originalConfig.isFile(), "Original repo.xml should be an existing file");
        assertTrue(mysqlConfig.isFile(), "MySQL repo.xml should be an existing file");

        //Create the different config dirs
        File repoDir = makeDirs(etcDir, "repo");
        makeDirs(repoDir, "derby");
        makeDirs(repoDir, "mysql");
        makeDirs(repoDir, "oracle");

        copyConfigToRepoDirs(originalConfig, repoDirs.toArray(new File[repoDirs.size()]));
        copyConfigToRepoDirs(mysqlConfig, homeDir);
        repoDirs.add(homeDir);
    }

    /**
     * Creates a directory and adds it to the repo dir set
     *
     * @param parent  Parent to create directory within
     * @param dirName Name of directory to create
     * @return Created dir
     */
    private File makeDirs(File parent, String dirName) {
        File dir = new File(parent, dirName);
        Assert.assertTrue(dir.mkdirs());
        repoDirs.add(dir);
        return dir;
    }

    /**
     * Copies the given file to the given dirs
     *
     * @param originalConfig Config to copy to all the dirs
     * @param dirs           Dirs to copy file to
     */
    private void copyConfigToRepoDirs(File originalConfig, File... dirs) throws IOException {
        for (File repoDir : dirs) {
            File copiedConfig = new File(repoDir, ArtifactoryHome.ARTIFACTORY_JCR_FILE);
            FileUtils.copyFile(originalConfig, copiedConfig);
            assertTrue(copiedConfig.isFile(), "Original repo.xml should have been copied.");
            assertTrue(FileUtils.contentEquals(originalConfig, copiedConfig), "Copied repo.xml should have" +
                    " content equals to the source file.");
        }
    }
}
