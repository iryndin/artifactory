package org.artifactory.util;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Yossi Shaul
 */
@Test
public class FileUtilsTest {
    private File baseTestDir;

    @BeforeMethod
    public void createTempDir() {
        baseTestDir = new File(System.getProperty("java.io.tmpdir"), "fileutilstest");
        Assert.assertTrue(baseTestDir.mkdirs(), "Failed to create base test dir");
    }

    @AfterMethod
    public void dleteTempDir() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(baseTestDir);
    }

    public void cleanupEmptyDirectoriesNonExistentDir() {
        File nonExistentFile = new File("pampam123");
        Assert.assertFalse(nonExistentFile.exists());
        FileUtils.cleanupEmptyDirectories(nonExistentFile);
    }

    public void cleanupEmptyDirectoriesEmptyDir() {
        FileUtils.cleanupEmptyDirectories(baseTestDir);
        Assert.assertTrue(baseTestDir.exists(), "Method should not delete base directory");
        Assert.assertEquals(baseTestDir.listFiles().length, 0, "Expected empty directory");
    }

    public void cleanupEmptyDirectoriesDirWithEmptyNestedDirectories() {
        File nested1 = createNestedDirectory("org/test");
        File nested2 = createNestedDirectory("org/apache");
        createNestedDirectory("org/apache/empty");

        FileUtils.cleanupEmptyDirectories(baseTestDir);

        Assert.assertTrue(baseTestDir.exists(), "Method should not delete base directory");
        Assert.assertFalse(nested1.exists() || nested2.exists(), "Nested empty directory wasn't deleted");
        File[] files = baseTestDir.listFiles();
        Assert.assertEquals(files.length, 0, "Expected empty directory but received: " + Arrays.asList(files));
    }

    public void cleanupEmptyDirectoriesDirWithFiles() throws IOException {
        File nested1 = createNestedDirectory("org/test");
        File nested2 = createNestedDirectory("org/apache");
        // create empty file
        File file = new File(nested2, "emptyfile");
        org.apache.commons.io.FileUtils.touch(file);

        FileUtils.cleanupEmptyDirectories(baseTestDir);

        Assert.assertTrue(baseTestDir.exists(), "Method should not delete base directory");
        Assert.assertFalse(nested1.exists(), "Nested empty directory wasn't deleted");
        Assert.assertTrue(nested2.exists(), "Nested directory was deleted but wasn't empty");
        Assert.assertEquals(nested2.listFiles().length, 1, "One file expected");
        Assert.assertEquals(nested2.listFiles()[0], file, "Unexpected file found " + file);
        File[] files = baseTestDir.listFiles();
        Assert.assertEquals(files.length, 1, "Expected 1 directory but received: " + Arrays.asList(files));
    }

    private File createNestedDirectory(String relativePath) {
        File nested = new File(baseTestDir, relativePath);
        Assert.assertTrue(nested.mkdirs(), "Failed to create nested directory" + nested);
        return nested;
    }

}
