/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.storage;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test the {@link org.artifactory.storage.StorageUnit} enum
 *
 * @author Tomer Cohen
 */
@Test
public class StorageUnitTest {

    private static final long twoGbInBytes = 2147483648L;
    private static final long twoAndAHalfGigsInBytes = 2684354560L;
    private static final long twoMbInBytes = 2097152L;

    public void convertBytesToGb() {
        double bytesInGiga = StorageUnit.GB.fromBytes(twoGbInBytes);
        assertEquals(bytesInGiga, 2.0, "The convert bytes in giga don't match");
    }

    public void revertGbToBytes() {
        double gigasInBytes = StorageUnit.GB.toBytes(2);
        assertEquals(gigasInBytes, 2147483648.0, "The convert giga in bytes don't match");
    }

    public void convertBytesToMb() {
        double bytesInGiga = StorageUnit.MB.fromBytes(twoMbInBytes);
        assertEquals(bytesInGiga, 2.0, "The convert bytes in giga don't match");
    }

    public void revertMbToBytes() {
        double gigasInBytes = StorageUnit.MB.toBytes(2);
        assertEquals(gigasInBytes, 2097152.0, "The convert giga in bytes don't match");
    }

    public void bytesToReadableFormat() {
        String megabytes = StorageUnit.toReadableString(twoMbInBytes);
        assertEquals(megabytes, "2.00 MB");

        String gigabytes = StorageUnit.toReadableString(twoGbInBytes);
        assertEquals(gigabytes, "2.00 GB");

        String twoAndAHalfGigs = StorageUnit.toReadableString(twoAndAHalfGigsInBytes);
        assertEquals(twoAndAHalfGigs, "2.50 GB");
    }
}
