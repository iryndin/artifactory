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
package org.artifactory.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ben Walding
 */
public class MimeTypes {
    private static Map<String, MimeType> mimeTypesByExtension = new HashMap<String, MimeType>();

    public static class MimeType {
        private final String mimeType;

        public MimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    static {
        mimeTypesByExtension.put(".jar", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".war", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".ear", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".sar", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".nar", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".so", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".dll", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".jam", new MimeType("application/octet-stream"));
        mimeTypesByExtension.put(".md5", new MimeType("text/plain"));
    }

    public static MimeType getMimeTypeByExtension(String extension) {
        return mimeTypesByExtension.get(extension);
    }

    public static MimeType getMimeTypeByPath(String path) {
        String extension = getExtension(path);
        return mimeTypesByExtension.get(extension);
    }

    public static String getMimeTypeByPathAsString(String path) {
        MimeType mimeType = getMimeTypeByPath(path);
        return mimeType != null ? mimeType.getMimeType() : null;
    }

    /**
     * @param path
     * @return
     */
    public static String getExtension(String path) {
        int dotPos = path.lastIndexOf('.');
        if (dotPos < 0) {
            return null;
        }
        return path.substring(dotPos);
    }

}