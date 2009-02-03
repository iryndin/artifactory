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

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public enum ChecksumType {
    md5("MD5", ".md5"), sha1("SHA-1", ".sha1");

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
}
