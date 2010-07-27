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

import org.artifactory.api.mime.ChecksumType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Checksum {

    private final ChecksumType type;
    private final MessageDigest digest;
    private String checksum;

    /**
     * @param type The checksum type
     */
    public Checksum(ChecksumType type) {
        this.type = type;
        String algorithm = type.alg();
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(
                    "Cannot create a digest for algorithm: " + algorithm);
        }
    }

    public ChecksumType getType() {
        return type;
    }

    public String getChecksum() {
        return checksum;
    }

    void update(byte[] bytes, int length) {
        digest.update(bytes, 0, length);
    }

    void calc() {
        if (checksum != null) {
            throw new IllegalStateException("Checksum already calculated.");
        }
        //Encodes a 128 bit or 160-bit byte array into a String
        byte[] bytes = digest.digest();
        if (bytes.length != 16 && bytes.length != 20) {
            int bitLength = bytes.length * 8;
            throw new IllegalArgumentException("Unrecognised length for binary data: " + bitLength + " bits");
        }
        StringBuilder sb = new StringBuilder();
        for (byte aBinaryData : bytes) {
            String t = Integer.toHexString(aBinaryData & 0xff);
            if (t.length() == 1) {
                sb.append("0");
            }
            sb.append(t);
        }
        checksum = sb.toString().trim();
    }
}
