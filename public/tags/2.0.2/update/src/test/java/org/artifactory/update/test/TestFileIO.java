package org.artifactory.update.test;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.File;

/**
 * User: freds
 * Date: Jun 11, 2008
 * Time: 3:26:22 PM
 */
public class TestFileIO {
    @Test
    public void testFileReturns() throws Exception {
        String tmpFileName = System.getProperty("java.io.tmpdir");
        assertNotNull(tmpFileName);
        File tmpFolder = new File(tmpFileName);
        tmpFolder.deleteOnExit();
        assertTrue(tmpFolder.exists());
        File testFolder = new File(tmpFolder, "FolderTest-" + System.currentTimeMillis());
        assertFalse(testFolder.exists());
        assertTrue(testFolder.mkdir());
        assertTrue(testFolder.exists());
        // Returns false if it exists
        assertFalse(testFolder.mkdir());
    }
}
