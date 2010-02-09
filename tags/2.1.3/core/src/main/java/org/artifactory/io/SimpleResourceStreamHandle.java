/*
 * This file is part of Artifactory.
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

package org.artifactory.io;

import org.apache.commons.io.IOUtils;
import org.artifactory.common.ResourceStreamHandle;

import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class SimpleResourceStreamHandle implements ResourceStreamHandle {
    private final InputStream is;
    private final long size;

    public SimpleResourceStreamHandle(InputStream is) {
        this(is, -1);
    }

    public SimpleResourceStreamHandle(InputStream is, long size) {
        this.is = is;
        this.size = size;
    }

    public InputStream getInputStream() {
        return is;
    }

    public long getSize() {
        return size;
    }

    public void close() {
        IOUtils.closeQuietly(is);
    }
}