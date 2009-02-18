package org.artifactory.io.checksum;

import org.apache.commons.io.IOUtils;
import org.artifactory.api.mime.ChecksumType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Checksums calculations helper that wraps and uses the ChecksumInputStream.
 *
 * @author Yossi Shaul
 */
public class ChecksumCalculator {

    /**
     * Calculate checksums for all known checksum types.
     *
     * @param in    Input streams for which checksums are calculated
     * @return Array of all computed checksums
     */
    public static Checksum[] calculateAll(InputStream in) throws IOException {
        return calculate(in, ChecksumType.values());
    }

    /**
     * Calculate checksums for all the input types.
     *
     * @param in    Input streams for which checksums are calculated
     * @param types Checksum types to calculate
     * @return Array of all computed checksums
     * @throws IOException  On any exception reading from the stream
     */
    public static Checksum[] calculate(InputStream in, ChecksumType... types) throws IOException {
        Checksum[] checksums = new Checksum[types.length];
        for (int i = 0; i < types.length; i++) {
            checksums[i] = new Checksum(types[i]);
        }

        ChecksumInputStream checksumsInputStream = new ChecksumInputStream(in, checksums);
        byte[] bytes = new byte[1024];
        while (checksumsInputStream.read(bytes) > 0) {
            // nothing to do, checksum output stream calculates the checksums
        }
        return checksums;
    }


    /**
     * Calculate checksums for all the input types.
     *
     * @param file    File for which checksums are calculated
     * @param types Checksum types to calculate
     * @return Array of all computed checksums
     * @throws IOException  On any exception reading from the file
     */
    public static Checksum[] calculate(File file, ChecksumType[] types) throws IOException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            return calculate(fileInputStream, types);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }
}
