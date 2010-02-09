package org.artifactory.descriptor;

import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the DescriptionExtractor.
 *
 * @author Yossi Shaul
 */
@Test
public class DescriptionExtractorTest {
    protected DescriptionExtractor extractor;

    @BeforeClass
    private void setup() {
        extractor = DescriptionExtractor.getInstance();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void noSuchProperty() {
        IndexerDescriptor indexer = new IndexerDescriptor();

        extractor.getDescription(indexer, "momo");
    }

    public void simpleProperty() {
        CentralConfigDescriptorImpl cc = new CentralConfigDescriptorImpl();

        String description = extractor.getDescription(cc, "fileUploadMaxSizeMb");

        Assert.assertNotNull(description, "Description should not be null");
        Assert.assertTrue(description.startsWith(
                "The maximun size in megabytes for uploaded artifact files."));
    }

    public void simplePropertyDescriptionWithinCData() {
        CentralConfigDescriptorImpl cc = new CentralConfigDescriptorImpl();

        String description = extractor.getDescription(cc, "dateFormat");

        Assert.assertNotNull(description, "Description should not be null");
        Assert.assertFalse(description.contains("[#text:") || description.contains("CDATA"),
                "Looks like the query didn't get the resolved xml text");
        Assert.assertEquals(description, "The format used for displaying dates.");
    }

    public void inheritedProperty() {
        HttpRepoDescriptor httpRepo = new HttpRepoDescriptor();
        String description = extractor.getDescription(httpRepo, "offline");

        Assert.assertNotNull(description, "Description should not be null");
        Assert.assertTrue(description.startsWith("When set to true only"), "Description not match");
    }

    public void propertyWithElementWrapper() {
        // test property that has the @WrapperElement annotation
        CentralConfigDescriptorImpl cc = new CentralConfigDescriptorImpl();
        String description = extractor.getDescription(cc, "backups");

        Assert.assertNotNull(description, "Description should not be null");
        Assert.assertEquals(description, "A set of backup configurations.", "Description not match");
    }

    public void propertyWithoutDescription() {
        CentralConfigDescriptorImpl cc = new CentralConfigDescriptorImpl();
        String description = extractor.getDescription(cc, "indexer");

        Assert.assertEquals(description, "", "Description should be empty");
    }

    public void xmlTypeFromClass() {
        String typeName = extractor.getComplexTypeName(CentralConfigDescriptorImpl.class);
        Assert.assertEquals(typeName, "CentralConfigType", "Wrong xml type");

        typeName = extractor.getComplexTypeName(BackupDescriptor.class);
        Assert.assertEquals(typeName, "BackupType", "Wrong xml type");

        typeName = extractor.getComplexTypeName(SearchPattern.class);
        Assert.assertEquals(typeName, "SearchType", "Wrong xml type");
    }

}
