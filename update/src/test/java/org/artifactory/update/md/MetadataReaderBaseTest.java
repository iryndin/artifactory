package org.artifactory.update.md;

import org.artifactory.api.md.MetadataEntry;

import java.util.List;

/**
 * Base class for the metadata reader tests.
 *
 * @author Yossi Shaul
 */
public abstract class MetadataReaderBaseTest {

    protected MetadataEntry getMetadataByName(List<MetadataEntry> entries, String metadataName) {
        for (MetadataEntry entry : entries) {
            if (entry.getMetadataName().equals(metadataName)) {
                return entry;
            }
        }
        // fail if not found
        org.testng.Assert.fail(String.format("Metadata %s not found in %s", metadataName, entries));
        return null;
    }
}
