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

/**
 * Represent the different storage units and their conversion to/from bytes
 * Based on {@link org.apache.wicket.util.lang.Bytes}
 *
 * @author Tomer Cohen
 */
public enum StorageUnit {
    /**
     * The kilobyte storage unit.
     */
    KB {
        @Override
        public double convert(long size) {
            return size / 1024.0;
        }
        @Override
        public double revert(int size) {
            return size * 1024.0;
        }},
    /**
     * The megabyte storage unit.
     */
    MB {
        @Override
        public double convert(long size) {
            return KB.convert(size) / 1024.0;
        }
        @Override
        public double revert(int size) {
            return KB.revert(size) * 1024.0;
        }},
    /**
     * The gigabyte storage unit.
     */
    GB {
        @Override
        public double convert(long size) {
            return MB.convert(size) / 1024.0;
        }
        @Override
        public double revert(int size) {
            return MB.revert(size) * 1024.0;
        }},

    /**
     * The terabyte storage unit.
     */
    TB {
        @Override
        public double convert(long size) {
            return GB.convert(size) / 1024.0;
        }
        @Override
        public double revert(int size) {
            return GB.revert(size) * 1024.0;
        }};

    /**
     * Convert the number of bytes to the target storage unit.
     *
     * @param size The initial number in bytes.
     * @return The converted number of bytes in the target storage unit.
     */
    public abstract double convert(long size);

    /**
     * Revert the number of the target storage unit to bytes.
     *
     * @param size The number of the target storage unit.
     * @return The converted number of target storage units back to bytes.
     */
    public abstract double revert(int size);
}
