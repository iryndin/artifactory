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

package org.artifactory.io.checksum;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yoav Landman
 */
public class ChecksumInputStream extends BufferedInputStream {

    private final Checksum[] checksums;

    public ChecksumInputStream(InputStream is, Checksum... checksums) {
        super(is);
        this.checksums = checksums;
    }

    public Checksum[] getChecksums() {
        return checksums;
    }

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read() throws IOException {
        byte b[] = new byte[1];
        return read(b);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead > 0) {
            for (Checksum checksum : checksums) {
                checksum.update(b, bytesRead);
            }
        } else {
            for (Checksum checksum : checksums) {
                checksum.calc();
            }
        }
        return bytesRead;
    }
}
