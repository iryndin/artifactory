package org.artifactory.io.checksum;

import org.artifactory.api.mime.ChecksumType;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Tests the ChecksumCalculator.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumCalculatorTest {

    public void calculateSha1() throws IOException {
        byte[] bytes = "this is a test".getBytes();
        Checksum[] results = ChecksumCalculator.calculate(new ByteArrayInputStream(bytes), ChecksumType.sha1);
        assertNotNull(results, "Results should not be null");
        assertEquals(results.length, 1, "Expecting only one calculated value");
        assertEquals(results[0].getChecksum(), "fa26be19de6bff93f70bc2308434e4a440bbad02",
                "Wrong SHA1 calculated");
    }

    public void calculateMd5() throws IOException {
        byte[] bytes = "this is a test".getBytes();
        Checksum[] results = ChecksumCalculator.calculate(new ByteArrayInputStream(bytes), ChecksumType.md5);
        assertNotNull(results, "Results should not be null");
        assertEquals(results.length, 1, "Expecting only one calculated value");
        assertEquals(results[0].getChecksum(), "54b0c58c7ce9f2a8b551351102ee0938",
                "Wrong SHA1 calculated");
    }

    public void calculateSha1AndMd5() throws IOException {
        byte[] bytes = "and this is another test".getBytes();
        Checksum[] results = ChecksumCalculator.calculate(new ByteArrayInputStream(bytes),
                ChecksumType.sha1, ChecksumType.md5);
        assertNotNull(results, "Results should not be null");
        assertEquals(results.length, 2, "Expecting two calculated value");
        assertEquals(results[0].getChecksum(), "5258d99970d60aed055c0056a467a0422acf7cb8",
                "Wrong SHA1 calculated");
        assertEquals(results[1].getChecksum(), "72f1aea68f75f79889b99cd4ff7acc83",
                "Wrong MD5 calculated");
    }

    public void calculateAllKnownChecksums() throws IOException {
        byte[] bytes = "and this is another test".getBytes();
        Checksum[] results = ChecksumCalculator.calculateAll(new ByteArrayInputStream(bytes));
        assertNotNull(results, "Results should not be null");
        assertEquals(results.length, ChecksumType.values().length, "Expecting two calculated value");
        assertEquals(results[0].getChecksum(), "5258d99970d60aed055c0056a467a0422acf7cb8",
                "Wrong SHA1 calculated");
        assertEquals(results[1].getChecksum(), "72f1aea68f75f79889b99cd4ff7acc83",
                "Wrong MD5 calculated");
    }

}
