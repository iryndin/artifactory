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
public abstract class MimeTypes {
    private static Map<String, MimeType> mimeTypesByExtension = new HashMap<String, MimeType>();

    public static class MimeType {
        private final String mimeType;
        private final boolean archive;

        public MimeType(String mimeType, boolean archiveType) {
            this.mimeType = mimeType;
            this.archive = archiveType;
        }

        public String getMimeType() {
            return mimeType;
        }

        public boolean isArchive() {
            return archive;
        }
    }

    static {
        mimeTypesByExtension.put("jar", new MimeType("application/octet-stream", true));
        mimeTypesByExtension.put("war", new MimeType("application/octet-stream", true));
        mimeTypesByExtension.put("ear", new MimeType("application/octet-stream", true));
        mimeTypesByExtension.put("sar", new MimeType("application/octet-stream", true));
        mimeTypesByExtension.put("nar", new MimeType("application/octet-stream", true));
        mimeTypesByExtension.put("so", new MimeType("application/octet-stream", false));
        mimeTypesByExtension.put("dll", new MimeType("application/octet-stream", false));
        mimeTypesByExtension.put("jam", new MimeType("application/octet-stream", true));
        mimeTypesByExtension.put("md5", new MimeType("text/plain", false));
        mimeTypesByExtension.put("xml", new MimeType("text/xml", false));
    }

    public static MimeType getMimeTypeByExtension(String extension) {
        return mimeTypesByExtension.get(extension);
    }

    public static MimeType getMimeTypeByPath(String path) {
        String extension = PathUtils.getExtension(path);
        return mimeTypesByExtension.get(extension);
    }

    public static String getMimeTypeByPathAsString(String path) {
        MimeType mimeType = getMimeTypeByPath(path);
        return mimeType != null ? mimeType.getMimeType() : null;
    }

    public static boolean isJarVariant(String path) {
        MimeType mt = getMimeTypeByPath(path);
        return mt != null && mt.isArchive();
    }

}