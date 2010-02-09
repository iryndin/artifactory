/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.io.checksum;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Checksum {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Checksum.class);

    private final String name;
    private final ChecksumType type;
    private final MessageDigest digest;
    private String checksum;

    public Checksum(String name, ChecksumType type) {
        this.name = name;
        this.type = type;
        String algorithm = type.alg();
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(
                    "Cannot create a digest for algorithm: " + algorithm);
        }
    }

    public String getName() {
        return name;
    }

    public ChecksumType getType() {
        return type;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getFileName() {
        return name + type.ext();
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public InputStream asInputStream() throws UnsupportedEncodingException {
        if (checksum == null) {
            throw new IllegalStateException("Checksum has not been calculated yet.");
        }
        byte[] checksumAsBytes = checksum.getBytes("ISO-8859-1");
        ByteArrayInputStream bais = new ByteArrayInputStream(checksumAsBytes);
        return bais;
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
            throw new IllegalArgumentException(
                    "Unrecognised length for binary data: " + bitLength + " bits");
        }
        String retValue = "";
        for (byte aBinaryData : bytes) {
            String t = Integer.toHexString(aBinaryData & 0xff);
            if (t.length() == 1) {
                retValue += ("0" + t);
            } else {
                retValue += t;
            }
        }
        checksum = retValue.trim();
    }
}
