/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.io.checksum;

import org.apache.commons.io.IOUtils;
import org.artifactory.checksum.ChecksumType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Checksums calculations helper that wraps and uses the ChecksumInputStream.
 *
 * @author Yossi Shaul
 */
public abstract class Checksums {
    private Checksums() {
        // utility class
    }

    /**
     * Calculate checksums for all known checksum types.
     *
     * @param in Input streams for which checksums are calculated
     * @return Array of all computed checksums
     */
    public static Checksum[] calculateAll(InputStream in) throws IOException {
        return calculate(in, ChecksumType.values());
    }

    /**
     * Calculate checksum for the input type.
     *
     * @param in   Input streams for which checksums are calculated
     * @param type Checksum type to calculate
     * @return The computed checksum
     * @throws IOException On any exception reading from the stream
     */
    public static Checksum calculate(InputStream in, ChecksumType type) throws IOException {
        return calculate(in, new ChecksumType[]{type})[0];
    }

    /**
     * Calculate checksums for all the input types.
     *
     * @param in    Input streams for which checksums are calculated
     * @param types Checksum types to calculate
     * @return Array of all computed checksums
     * @throws IOException On any exception reading from the stream
     */
    public static Checksum[] calculate(InputStream in, ChecksumType... types) throws IOException {
        Checksum[] checksums = new Checksum[types.length];
        for (int i = 0; i < types.length; i++) {
            checksums[i] = new Checksum(types[i]);
        }

        ChecksumInputStream checksumsInputStream = new ChecksumInputStream(in, checksums);
        byte[] bytes = new byte[1024];
        while (checksumsInputStream.read(bytes) != -1) {
            // nothing to do, checksum output stream calculates the checksums
        }
        checksumsInputStream.close();
        return checksums;
    }

    /**
     * Calculate checksum for the input type.
     *
     * @param in   File for which checksums are calculated
     * @param type Checksum type to calculate
     * @return The computed checksum
     * @throws IOException On any exception reading from the stream
     */
    public static Checksum calculate(File file, ChecksumType type) throws IOException {
        return calculate(file, new ChecksumType[]{type})[0];
    }

    /**
     * Calculate checksums for all the input types.
     *
     * @param file  File for which checksums are calculated
     * @param types Checksum types to calculate
     * @return Array of all computed checksums
     * @throws IOException On any exception reading from the file
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
