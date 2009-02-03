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
package org.artifactory.checksum;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ChecksumInputStream extends BufferedInputStream {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ChecksumInputStream.class);

    private final Checksum[] checksums;

    public ChecksumInputStream(InputStream is, Checksum... checksums) {
        super(is);
        this.checksums = checksums;
    }

    public Checksum[] getChecksums() {
        return checksums;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read() throws IOException {
        byte b[] = new byte[1];
        return read(b);
    }

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
