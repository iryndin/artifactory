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
package org.artifactory.api.common;

import org.artifactory.utils.PathUtils;

/**
 * Used when deploying manually to artifactory, and classifiyng pom files. Only jar are queried to
 * contain a pom file. Created by IntelliJ IDEA. User: yoavl
 */
public enum PackagingType {
    // TODO: Supports more packaging type like zip that can contains pom also
    jar(ContentType.applicationOctetStream, true, true, false),
    pom(ContentType.textXml, false, false, false),
    xml(ContentType.textXml, false, false, false),
    xsd(ContentType.textXml, false, false, false),
    md5(ContentType.textPlain, false, false, true),
    sha1(ContentType.textPlain, false, false, true),
    asc(ContentType.textPlain, false, false, true);

    public static final String METADATA_PREFIX = "maven-metadata";

    public static PackagingType getPackagingTypeByPath(String path) {
        String extension = PathUtils.getExtension(path);
        try {
            if (extension != null && extension.length() > 0) {
                return valueOf(extension);
            }
        } catch (IllegalArgumentException ignore) {
            // TODO: Find something else than coding by exception
        }
        return null;
    }

    public static String getMimeTypeByPathAsString(String path) {
        PackagingType pt = getPackagingTypeByPath(path);
        return pt != null ? pt.getContentType().getMimeType() : null;
    }

    public static boolean isJarVariant(String path) {
        PackagingType pt = getPackagingTypeByPath(path);
        return pt != null && pt.isArchive();
    }

    public static boolean isChecksum(String path) {
        PackagingType pt = getPackagingTypeByPath(path);
        return pt != null && pt.isChecksum();
    }

    public static boolean isPom(String path) {
        PackagingType pt = getPackagingTypeByPath(path);
        return pt != null && pt == PackagingType.pom;
    }

    public static boolean isMavenMetadata(String path) {
        String fileName = PathUtils.getName(path);
        if (fileName == null || fileName.length() == 0) {
            return false;
        }
        PackagingType pt = getPackagingTypeByPath(fileName);
        return pt != null && pt == PackagingType.xml && fileName.startsWith(METADATA_PREFIX);
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

    private final ContentType contentType;
    private final boolean archive;
    private final boolean jarVariant;
    private final boolean checksum;

    PackagingType(ContentType contentType, boolean archive, boolean jarVariant, boolean checksum) {
        this.contentType = contentType;
        this.archive = archive;
        this.jarVariant = jarVariant;
        this.checksum = checksum;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public boolean isArchive() {
        return archive;
    }

    public boolean isJarVariant() {
        return jarVariant;
    }

    public boolean isChecksum() {
        return checksum;
    }

    public boolean isXml() {
        return contentType.isXml();
    }
}
