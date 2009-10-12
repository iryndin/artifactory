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
package org.artifactory.api.mime;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public enum ChecksumType {
    sha1("SHA-1", ".sha1"), md5("MD5", ".md5");

    private final String alg;
    private final String ext;

    ChecksumType(String alg, String ext) {
        this.alg = alg;
        this.ext = ext;
    }

    public String alg() {
        return alg;
    }

    public String ext() {
        return ext;
    }

    /**
     * @param ext   The checksum filename extension assumed to start with '.' for example '.sha1'.
     * @return Checksum type for the given extension. Null if not found.
     */
    public static ChecksumType forExtension(String ext) {
        if (sha1.ext.equals(ext)) {
            return sha1;
        } else if (md5.ext.equals(ext)) {
            return md5;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return alg;
    }
}
