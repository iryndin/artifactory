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

package org.artifactory.api.storage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

interface StorageConstants {
    /**
     * One kilo bytes in big decimal
     */
    BigDecimal ONE_KILO = new BigDecimal(1024);

    /**
     * The number of units to the transition to the next unit, i.e. there are 1024 bytes in a KB.
     */
    double UNITS_TO_NEXT_UNIT = 1024.0;
}

/**
 * Represent the different storage units and their conversion to/from bytes
 *
 * @author Tomer Cohen
 */
public enum StorageUnit {
    /**
     * One byte
     */
    ONE(new BigDecimal(1)) {
        @Override
        public double fromBytes(long size) {
            return size;
        }

        @Override
        public double toBytes(int size) {
            return size;
        }

        @Override
        public String displayName() {
            return "bytes";
        }
    },
    /**
     * The kilobyte storage unit.
     */
    KB(StorageConstants.ONE_KILO) {
        @Override
        public double fromBytes(long size) {
            return size / StorageConstants.UNITS_TO_NEXT_UNIT;
        }

        @Override
        public double toBytes(int size) {
            return size * StorageConstants.UNITS_TO_NEXT_UNIT;
        }
    },
    /**
     * The megabyte storage unit.
     */
    MB(KB.getNumberOfBytes().multiply(StorageConstants.ONE_KILO)) {
        @Override
        public double fromBytes(long size) {
            return KB.fromBytes(size) / StorageConstants.UNITS_TO_NEXT_UNIT;
        }

        @Override
        public double toBytes(int size) {
            return KB.toBytes(size) * StorageConstants.UNITS_TO_NEXT_UNIT;
        }
    },
    /**
     * The gigabyte storage unit.
     */
    GB(MB.getNumberOfBytes().multiply(StorageConstants.ONE_KILO)) {
        @Override
        public double fromBytes(long size) {
            return MB.fromBytes(size) / StorageConstants.UNITS_TO_NEXT_UNIT;
        }

        @Override
        public double toBytes(int size) {
            return MB.toBytes(size) * StorageConstants.UNITS_TO_NEXT_UNIT;
        }
    },

    /**
     * The terabyte storage unit.
     */
    TB(GB.getNumberOfBytes().multiply(StorageConstants.ONE_KILO)) {
        @Override
        public double fromBytes(long size) {
            return GB.fromBytes(size) / StorageConstants.UNITS_TO_NEXT_UNIT;
        }

        @Override
        public double toBytes(int size) {
            return GB.toBytes(size) * StorageConstants.UNITS_TO_NEXT_UNIT;
        }
    };

    private final BigDecimal numberOfBytes;

    StorageUnit(BigDecimal numberOfBytes) {
        this.numberOfBytes = numberOfBytes;
    }

    public BigDecimal getNumberOfBytes() {
        return numberOfBytes;
    }

    /**
     * Convert the number of bytes to the target storage unit.
     *
     * @param size The initial number in bytes.
     * @return The converted number of bytes in the target storage unit.
     */
    public abstract double fromBytes(long size);

    /**
     * Revert the number of the target storage unit to bytes.
     *
     * @param size The number of the target storage unit.
     * @return The converted number of target storage units back to bytes.
     */
    public abstract double toBytes(int size);

    /**
     * @return How to display the size in readable format
     */
    public String displayName() {
        return name();
    }

    /**
     * Convert the number of bytes to a human readable size, if the size is more than 1024 megabytes display the correct
     * number of gigabytes.
     *
     * @param size The size in bytes.
     * @return The size in human readable format.
     */
    public static String toReadableString(long size) {
        DecimalFormat decimalFormat = createFormat();
        for (StorageUnit unit : StorageUnit.values()) {
            double readableSize = unit.fromBytes(size);
            if (unit == TB) {
                // no more always return
                return decimalFormat.format(readableSize) + " " + unit.displayName();
            }
            // if less than 1 byte, then simply return the bytes as it is
            if (readableSize < StorageConstants.UNITS_TO_NEXT_UNIT) {
                if (unit == ONE) {
                    return size + " " + unit.displayName();
                } else {
                    return decimalFormat.format(readableSize) + " " + unit.displayName();
                }
            }
        }
        throw new IllegalStateException("Could not reach here");
    }

    public static String format(double size) {
        return createFormat().format(size);
    }

    public static long fromReadableString(String humanReadableSize) {
        String number = humanReadableSize.replaceAll("([TtGgMmKkBb])", "");
        BigDecimal d = new BigDecimal(number);
        BigDecimal l = null;
        int unitLength = humanReadableSize.length() - number.length();
        int unitIndex = unitLength > 0 ? humanReadableSize.length() - unitLength : 0;
        switch (humanReadableSize.charAt(unitIndex)) {
            default:
                l = d;
                break;
            case 'K':
            case 'k':
                l = d.multiply(KB.getNumberOfBytes());
                break;
            case 'M':
            case 'm':
                l = d.multiply(MB.getNumberOfBytes());
                break;
            case 'G':
            case 'g':
                l = d.multiply(GB.getNumberOfBytes());
                break;
            case 'T':
            case 't':
                l = d.multiply(TB.getNumberOfBytes());
                break;
        }
        return l.longValue();
    }

    private static DecimalFormat createFormat() {
        DecimalFormat decimalFormat = new DecimalFormat(",###.##", new DecimalFormatSymbols(Locale.ENGLISH));
        decimalFormat.setMinimumFractionDigits(2);
        return decimalFormat;
    }
}
