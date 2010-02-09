package org.artifactory.api.config;

import org.apache.commons.lang.builder.EqualsBuilder;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Tests the ExportSettinds.
 *
 * @author Yossi Shaul
 */
@Test
public class ExportSettingsTest {

    public void fileConstructor() {
        File dir = new File("base");
        ExportSettings settings = new ExportSettings(dir);
        assertEquals(settings.getBaseDir(), dir);
        assertNotNull(settings.getTime());
        assertFalse(settings.isM2Compatible());
        assertFalse(settings.isCreateArchive());
        assertFalse(settings.isIgnoreRepositoryFilteringRulesOn());
        assertTrue(settings.isIncludeMetadata());
        assertFalse(settings.isIncremental());
    }

    public void copyConstructor() {
        File base = new File("bases");
        ExportSettings orig = new ExportSettings(base);
        orig.setIgnoreRepositoryFilteringRulesOn(true);
        orig.setIncremental(true);

        ExportSettings copy = new ExportSettings(base, orig);
        assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Reflection comparison after " +
                "copy constructor failed");

        copy = new ExportSettings(new File("base2"), orig);
        assertFalse(copy.getBaseDir().equals(base), "The copy constructor shouldn't copy the base dir");
    }

}
