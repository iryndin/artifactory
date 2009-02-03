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

import org.artifactory.api.maven.MavenNaming;
import org.artifactory.utils.PathUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Used when deploying manually to artifactory, and classifiyng pom files. Only jar are queried to
 * contain a pom file.
 *
 * @author freds
 * @author yoavl
 */
public class PackagingType {
    public static final String METADATA_PARAM = "metadata";

    private final Map<String, ContentType> contentTypePerExtension =
            new HashMap<String, ContentType>();

    private static final PackagingType instance = new PackagingType();
    private static final String METADATA_QUERY_PREFIX = "?" + METADATA_PARAM + "=";

    public static PackagingType get() {
        return instance;
    }

    private PackagingType() {
        final ContentType[] contentTypes = ContentType.values();
        for (ContentType type : contentTypes) {
            final MimeEntry mimeEntry = type.getMimeEntry();
            Collection<String> exts = mimeEntry.getFileExts();
            if (exts != null) {
                for (String ext : exts) {
                    contentTypePerExtension.put(ext, type);
                }
            }
        }
    }

    public static ContentType getContentType(File file) {
        return getContentType(file.getName());
    }

    public static ContentType getContentType(String path) {
        String extension = PathUtils.getExtension(path);
        return getContentTypeByExtension(extension);
    }

    public static ContentType getContentTypeByExtension(String extension) {
        ContentType result = null;
        if (extension != null && extension.length() > 0) {
            result = get().contentTypePerExtension.get(extension);
        }
        if (result == null) {
            return ContentType.def;
        } else {
            return result;
        }
    }

    public static String getMimeTypeByPathAsString(String path) {
        ContentType ct = getContentType(path);
        return ct.getMimeType();
    }

    public static boolean isChecksum(String path) {
        ContentType ct = getContentType(path);
        return ct.isChecksum();
    }

    public static boolean isMetadata(String path) {
        String fileName = PathUtils.getName(path);
        if (fileName == null || fileName.length() == 0) {
            return false;
        }
        //First check for the metadata pattern of x/y/z/artifactName?metadata=name
        if (fileName.contains(METADATA_QUERY_PREFIX)) {
            return true;
        }
        //Fallback to checking maven metadata
        ContentType ct = getContentType(path);
        return ct.isXml() && fileName.startsWith(MavenNaming.MAVEN_METADATA_PREFIX);
    }

    /**
     * Return the name of the requested metadata Should be called on a path after determining that
     * it is indeed a metadata path
     *
     * @param path
     * @return
     */
    public static String getMedtadataName(String path) {
        //First check for the metadata pattern of x/y/z/artifactName?metadata=name
        String name = getParameter(path, METADATA_PARAM);
        if (name == null) {
            //Fallback to checking maven metadata
            String fileName = PathUtils.getName(path);
            ContentType ct = getContentType(path);
            if (ct.isXml() && fileName.startsWith(MavenNaming.MAVEN_METADATA_PREFIX)) {
                return MavenNaming.MAVEN_METADATA_PREFIX;
            }
        }
        return null;
    }

    public static boolean isNonUniqueSnapshot(String path) {
        int idx = path.indexOf("-SNAPSHOT.");
        return idx > 0 && idx > path.lastIndexOf('/');
    }

    public static boolean isSnapshot(String path) {
        boolean result = isNonUniqueSnapshot(path);
        if (!result) {
            int versionIdx = path.indexOf("SNAPSHOT/");
            result = versionIdx > 0 && path.lastIndexOf('/') == versionIdx + 8;
        }
        return result;
    }

    private static String getParameter(String path, String paramName) {
        String fileName = PathUtils.getName(path);
        String paramQueryPrefix = paramName + "=";
        int mdStart = fileName.lastIndexOf(paramQueryPrefix);
        if (mdStart > 0) {
            int mdEnd = fileName.indexOf('&', mdStart);
            String paramValue = fileName.substring(mdStart + paramQueryPrefix.length(),
                    mdEnd > 0 ? mdEnd : fileName.length());
            return paramValue;
        }
        return null;
    }
}
