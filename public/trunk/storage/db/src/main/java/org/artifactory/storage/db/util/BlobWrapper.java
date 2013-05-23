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

package org.artifactory.storage.db.util;

import org.artifactory.util.StringInputStream;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * A wrapper around an input stream with the expected stream length.
 *
 * @author Yossi Shaul
 */
public class BlobWrapper {

    private final InputStream in;
    private final long length;

    /**
     * Build a wrapper around a string. The string is expected to be UTF-8 encoded.
     *
     * @param data The data to use as the input stream
     */
    public BlobWrapper(String data) throws UnsupportedEncodingException {
        if (data == null) {
            throw new NullPointerException("Data cannot be null");
        }
        StringInputStream sis = new StringInputStream(data);
        this.in = sis;
        this.length = sis.getLength();
    }

    public BlobWrapper(InputStream in) {
        this(in, -1);
    }

    public BlobWrapper(InputStream in, long length) {
        if (in == null) {
            throw new NullPointerException("Input stream cannot be null");
        }
        this.in = in;
        this.length = length;
    }

    public InputStream getInputStream() {
        return in;
    }

    public long getLength() {
        return length;
    }
}
