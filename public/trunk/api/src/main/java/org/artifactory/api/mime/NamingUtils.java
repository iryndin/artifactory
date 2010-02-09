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

package org.artifactory.api.mime;

import com.google.common.collect.Maps;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.util.Pair;
import org.artifactory.common.ConstantValues;
import org.artifactory.util.PathUtils;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Used when deploying manually to artifactory, and classifiyng pom files. Only jar are queried to contain a pom file.
 *
 * @author freds
 * @author yoavl
 */
public class NamingUtils {
    private static Map<String, ContentType> contentTypePerExtension;

    public static final String METADATA_PREFIX = ":";

    public static ContentType getContentType(File file) {
        return getContentType(file.getName());
    }

    /**
     * @param path A file path
     * @return The content type for the path. Will return default content type if not mapped.
     */
    public static ContentType getContentType(String path) {
        String extension = PathUtils.getExtension(path);
        return getContentTypeByExtension(extension);
    }

    public static ContentType getContentTypeByExtension(String extension) {
        if (contentTypePerExtension == null) {
            initializeContentTypesMap();
        }
        ContentType result = null;
        if (extension != null) {
            result = contentTypePerExtension.get(extension.toLowerCase());
        }

        return result != null ? result : ContentType.def;
    }

    public static String getMimeTypeByPathAsString(String path) {
        ContentType ct = getContentType(path);
        return ct.getMimeType();
    }

    public static boolean isChecksum(String path) {
        ContentType ct = getContentType(path);
        return ct.isChecksum();
    }

    public static Pair<String, String> getMetadtaNameAndParent(String path) {
        int mdPrefixIdx = path.lastIndexOf(METADATA_PREFIX);
        String name = null;
        String parent = null;
        if (mdPrefixIdx >= 0) {
            name = path.substring(mdPrefixIdx + METADATA_PREFIX.length());
            parent = path.substring(0, mdPrefixIdx);
        } else {
            //Fallback to checking maven metadata
            final File file = new File(path);
            if (MavenNaming.MAVEN_METADATA_NAME.equals(file.getName())) {
                name = MavenNaming.MAVEN_METADATA_NAME;
                parent = file.getParent();
            }
        }
        return new Pair<String, String>(name, parent);
    }

    public static boolean isMetadata(String path) {
        String fileName = PathUtils.getName(path);
        if (fileName == null || fileName.length() == 0) {
            return false;
        }
        //First check for the metadata pattern of x/y/z/resourceName:metadataName
        if (fileName.contains(METADATA_PREFIX)) {
            return true;
        }
        //Fallback to checking maven metadata
        return MavenNaming.isMavenMetadataFileName(fileName);
    }

    /**
     * @return True if the path points to a system file (e.g., maven index)
     */
    public static boolean isSystem(String path) {
        return MavenNaming.isIndex(path) || path.endsWith(".index");
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
        if (mdPrefixIdx >= 0) {
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

    public static String stripMetadataFromPath(String path) {
        int metadataPrefixIdx = path.lastIndexOf(NamingUtils.METADATA_PREFIX);
        if (metadataPrefixIdx >= 0) {
            path = path.substring(0, metadataPrefixIdx);
        }
        return path;
    }

    /**
     * Get the path of the metadata container. Assumes we already verified that this is a metadataPath.
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

    /**
     * Recieves a metadata container path (/a/b/c.pom) and a metadata name (maven-metadata.xml) and returns the whole
     * path - "/a/b/c.pom:maven-metadata.xml".
     *
     * @param containerPath Path of metadata container
     * @param metadataName  Name of metadata item
     * @return String - complete path to metadata
     */
    public static String getMetadataPath(String containerPath, String metadataName) {
        if ((containerPath == null) || (metadataName == null)) {
            throw new IllegalArgumentException("Container path and metadata name cannot be null.");
        }
        String metadataPath = containerPath + METADATA_PREFIX + metadataName;
        return metadataPath;
    }

    /**
     * Recieves a jcr path of a metadata item/element (.../a/b/c.pom/artifactory:xml/metadataname/element/xml:text) and
     * Extracts the name of the metadata item (metadataname, in this case).
     *
     * @param path A jcr path of a metadata element/item
     * @return String - Name of metadata item
     */
    public static String getMetadataNameFromJcrPath(String path) {
        //Build the metadata prefix to search for
        String metadatPrefix = METADATA_PREFIX + "metadata/";
        int prefixStart = path.indexOf(metadatPrefix);

        //If the prefix isn't in the path, it is either not a jcr path or not a metadata item/element
        if (prefixStart < 0) {
            return "";
        }

        //Dispose of all the path before the metadata name
        int prefixEnd = prefixStart + metadatPrefix.length();
        String metadataPath = path.substring(prefixEnd);

        //Find where the name ends (either a forward slash, or the end of the path)
        int followingSlash = metadataPath.indexOf('/');
        if (followingSlash < 0) {
            followingSlash = metadataPath.length();
        }
        String metadataName = metadataPath.substring(0, followingSlash);
        return metadataName;
    }

    public static void initializeContentTypesMap() {
        Map<String, ContentType> types = Maps.newHashMap();
        ContentType[] contentTypes = ContentType.values();
        for (ContentType type : contentTypes) {
            MimeEntry mimeEntry = type.getMimeEntry();
            Collection<String> exts = mimeEntry.getFileExts();
            if (exts != null) {
                for (String ext : exts) {
                    types.put(ext, type);
                }
            }
        }

        //add any xmlAdditionalMimeTypeExtensions from a.s.p to applicationXml content type
        String additionalMimeTypes = ConstantValues.xmlAdditionalMimeTypeExtensions.getString();
        if (additionalMimeTypes != null) {
            String[] exts = additionalMimeTypes.split(",");
            for (String ext : exts) {
                //for support *.ext
                String fileExt = PathUtils.getExtension(ext);
                types.put(fileExt != null ? fileExt : ext, ContentType.applicationXml);
            }
        }

        contentTypePerExtension = types;
    }
}
