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
import org.artifactory.util.PathUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Used when deploying manually to artifactory, and classifiyng pom files. Only jar are queried to contain a pom file.
 *
 * @author freds
 * @author yoavl
 */
public class NamingUtils {
    private static Map<String, ContentType> contentTypePerExtension = new HashMap<String, ContentType>();

    public static final String METADATA_PREFIX = "#";

    static {
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
            result = contentTypePerExtension.get(extension.toLowerCase());
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
        //First check for the metadata pattern of x/y/z/resourceName#metadataName
        if (fileName.contains(METADATA_PREFIX)) {
            return true;
        }
        //Fallback to checking maven metadata
        return MavenNaming.isMavenMetadataFileName(fileName);
    }

    /**
     * Return the name of the requested metadata. Should be called on a path after determining that it is indeed a
     * metadata path.
     * <pre>
     * getMetadataName("x/y/z/resourceName#file-info") = "file-info"
     * getMetadataName("x/y/z/resourceName/maven-metadata.xml") = "maven-metadata.xmlo"
     * </pre>
     *
     * @param path A metadata path in the pattern of x/y/z/resourceName#metadataName or a path that ends with
     *             maven-metadata.xml.
     * @return The metadata name from the path. Null if not valid.
     */
    public static String getMetadataName(String path) {
        //First check for the metadata pattern of x/y/z/resourceName#metadataName
        int mdPrefixIdx = path.lastIndexOf(METADATA_PREFIX);
        String name = null;
        if (mdPrefixIdx > 0) {
            name = path.substring(mdPrefixIdx + METADATA_PREFIX.length());
        } else {
            //Fallback to checking maven metadata
            String fileName = PathUtils.getName(path);
            if (MavenNaming.isMavenMetadataFileName(fileName)) {
                name = MavenNaming.MAVEN_METADATA_NAME;
            }
        }
        return name;
    }

    /**
     * Get the path of the metadata container Assumes we already verified that this is a metadataPath
     *
     * @param path
     * @return
     */
    public static String getMetadataParentPath(String path) {
        String metadataName = getMetadataName(path);
        return path.substring(0, path.lastIndexOf(metadataName) - 1);
    }

    public static boolean isSnapshotMetadata(String path) {
        //*-SNAPSHOT/*maven-metadata.xml or *-SNAPSHOT#maven-metadata.xml
        if (!isMetadata(path)) {
            return false;
        }
        String parent = getMetadataParentPath(path);
        return parent != null && parent.endsWith("-SNAPSHOT");
    }

    @SuppressWarnings({"UnusedDeclaration"})
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

    public static boolean isHidden(String path) {
        return path.startsWith(".");
    }
}
