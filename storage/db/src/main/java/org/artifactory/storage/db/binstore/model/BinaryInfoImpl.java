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

package org.artifactory.storage.db.binstore.model;

import org.apache.commons.lang.StringUtils;
import org.artifactory.binstore.BinaryInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.io.checksum.Sha1Md5ChecksumInputStream;

/**
 * Holds binary checksums and length
 *
 * @author Yossi Shaul
 */
public class BinaryInfoImpl implements BinaryInfo {

    private final String sha1;
    private final String md5;
    private final long length;

    public BinaryInfoImpl(Sha1Md5ChecksumInputStream checksumInputStream) {
        this.sha1 = checksumInputStream.getSha1();
        this.md5 = checksumInputStream.getMd5();
        this.length = checksumInputStream.getTotalBytesRead();
        simpleValidation();
    }

    public BinaryInfoImpl(String sha1, String md5, long length) {
        this.sha1 = sha1;
        this.md5 = md5;
        this.length = length;
        simpleValidation();
    }

    private void simpleValidation() {
        if (StringUtils.isBlank(sha1) || sha1.length() != ChecksumType.sha1.length()) {
            throw new IllegalArgumentException("SHA1 value '" + sha1 + "' is not a valid checksum");
        }
        if (StringUtils.isBlank(md5) || md5.length() != ChecksumType.md5.length()) {
            throw new IllegalArgumentException("MD5 value '" + md5 + "' is not a valid checksum");
        }
        if (length < 0L) {
            throw new IllegalArgumentException("Length " + length + " is not a valid length");
        }
    }

    public boolean isValid() {
        simpleValidation();
        return ChecksumType.sha1.isValid(sha1) && ChecksumType.md5.isValid(md5);
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public String getMd5() {
        return md5;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return sha1.equals(((BinaryInfoImpl) o).sha1);
    }

    @Override
    public int hashCode() {
        return sha1.hashCode();
    }
}
