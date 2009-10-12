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
package org.artifactory.util;

import java.io.File;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * User: freds Date: Aug 3, 2008 Time: 5:42:55 PM
 */
public class PathUtils {
    private static String HOST_ID;

    /**
     * Check that the given CharSequence is neither <code>null</code> nor of length 0. Note: Will return
     * <code>true</code> for a CharSequence that purely consists of whitespace.
     * <p><pre>
     * StringUtils.hasLength(null) = false
     * StringUtils.hasLength("") = false
     * StringUtils.hasLength(" ") = true
     * StringUtils.hasLength("Hello") = true
     * </pre>
     *
     * @param str the CharSequence to check (may be <code>null</code>)
     * @return <code>true</code> if the CharSequence is not null and has length
     * @see #hasText(String)
     */
    public static boolean hasLength(String str) {
        return (str != null && str.length() > 0);
    }

    /**
     * Check whether the given CharSequence has actual text. More specifically, returns <code>true</code> if the string
     * not <code>null</code>, its length is greater than 0, and it contains at least one non-whitespace character.
     * <p><pre>
     * StringUtils.hasText(null) = false
     * StringUtils.hasText("") = false
     * StringUtils.hasText(" ") = false
     * StringUtils.hasText("12345") = true
     * StringUtils.hasText(" 12345 ") = true
     * </pre>
     *
     * @param str the CharSequence to check (may be <code>null</code>)
     * @return <code>true</code> if the CharSequence is not <code>null</code>, its length is greater than 0, and it does
     *         not contain whitespace only
     * @see java.lang.Character#isWhitespace
     */
    public static boolean hasText(String str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trim leading and trailing whitespace from the given String.
     *
     * @param str the String to check
     * @return the trimmed String
     * @see java.lang.Character#isWhitespace
     */
    public static String trimWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }
        StringBuffer buf = new StringBuffer(str);
        while (buf.length() > 0 && Character.isWhitespace(buf.charAt(0))) {
            buf.deleteCharAt(0);
        }
        while (buf.length() > 0 && Character.isWhitespace(buf.charAt(buf.length() - 1))) {
            buf.deleteCharAt(buf.length() - 1);
        }
        return buf.toString();
    }

    public static String getPathFirstPart(String path) {
        String pathPrefix = null;
        if (path != null) {
            int pathPrefixEnd = path.indexOf('/', 1);
            if (pathPrefixEnd > 0) {
                if (path.startsWith("/")) {
                    pathPrefix = path.substring(1, pathPrefixEnd);
                } else {
                    pathPrefix = path.substring(0, pathPrefixEnd);
                }
            } else if (path.startsWith("/")) {
                pathPrefix = path.substring(1);
            } else {
                pathPrefix = path;
            }
        }
        return pathPrefix;
    }

    /**
     * @param path A file like path
     * @return The name of the path as if it was a file (the string after the last '/' or '\')
     */
    public static String getName(String path) {
        if (path == null) {
            return null;
        }
        File dummy = new File(path);
        return dummy.getName();
    }

    public static String getParent(String path) {
        if (path == null) {
            return null;
        }
        File dummy = new File(path);
        return formatPath(dummy.getParent());
    }

    /**
     * @param path The path (usually of a file)
     * @return The file extension. Null if file name has no extension. For example 'file.xml' will return xml, 'file'
     *         will return null.
     */
    public static String getExtension(String path) {
        if (path == null) {
            return null;
        }
        // TODO: check there is no slash after this dot
        int dotPos = path.lastIndexOf('.');
        if (dotPos < 0) {
            return null;
        }
        return path.substring(dotPos + 1);
    }

    /**
     * @param path The path (usually of a file)
     * @return The path without the extension. If the path has no extension the same path will be returned.
     *         <p/>
     *         For example 'file.xml' will return 'file', 'file' will return 'file'.
     */
    public static String stripExtension(String path) {
        String result = path;
        String extension = getExtension(path);
        if (extension != null) {
            result = path.substring(0, path.length() - extension.length() - 1);
        }
        return result;
    }

    /**
     * Caculate a unique id for the VM to support Artifactories with the same ip (e.g. accross NATs)
     */
    public static String getHostId() {
        if (HOST_ID == null) {
            VMID vmid = new VMID();
            HOST_ID = vmid.toString();
        }
        return HOST_ID;
    }

    public static String collectionToDelimitedString(Collection<String> coll, String delim) {
        if (coll == null || coll.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = coll.iterator();
        while (it.hasNext()) {
            String str = it.next();
            if (str == null) {
                continue;
            }
            str = str.trim();
            if (str.length() == 0) {
                continue;
            }
            sb.append(str);
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public static List<String> delimitedListToStringList(String str, String delimiter) {
        return delimitedListToStringList(str, delimiter, "\r\n\f\t ");
    }

    public static List<String> delimitedListToStringList(String str, String delimiter, String charsToDelete) {
        List<String> result = new ArrayList<String>();
        if (str == null) {
            return result;
        }
        if (delimiter == null) {
            result.add(str);
            return result;
        }
        if ("".equals(delimiter)) {
            for (int i = 0; i < str.length(); i++) {
                result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
            }
        } else {
            int pos = 0;
            int delPos;
            while ((delPos = str.indexOf(delimiter, pos)) != -1) {
                result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
                pos = delPos + delimiter.length();
            }
            if (str.length() > 0 && pos <= str.length()) {
                // Add rest of String, but not in case of empty input.
                result.add(deleteAny(str.substring(pos), charsToDelete));
            }
        }
        return result;
    }

    public static String deleteAny(String inString, String charsToDelete) {
        if (!hasLength(inString) || !hasLength(charsToDelete)) {
            return inString;
        }
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < inString.length(); i++) {
            char c = inString.charAt(i);
            if (charsToDelete.indexOf(c) == -1) {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String formatRelativePath(String path) {
        path = formatPath(path);
        //Trim leading '/' (caused by webdav requests)
        return trimLeadingSlashes(path);
    }

    /**
     * Replaces all backslashes in the path to slashes.
     *
     * @param path A path to format
     * @return The input path with all backslashes replaced with slashes. Return empty string if the input path is
     *         null.
     */
    public static String formatPath(String path) {
        if (PathUtils.hasText(path)) {
            if (path.contains("\\")) {
                return path.replace('\\', '/');
            }
            return path;
        } else {
            return "";
        }
    }

    public static String trimLeadingSlashes(String path) {
        //Trim leading '/' (caused by webdav requests)
        if (path.startsWith("/")) {
            String modifiedPath = path.substring(1);
            return trimLeadingSlashes(modifiedPath);
        }
        return path;
    }

    public static String trimTrailingSlashes(String absPath) {
        if (absPath.endsWith("/")) {
            String modifiedPath = absPath.substring(0, absPath.length() - 1);
            return trimTrailingSlashes(modifiedPath);
        }
        return absPath;
    }

    @SuppressWarnings({"StringEquality"})
    public static boolean safeStringEquals(String s1, String s2) {
        return s1 == s2 || (s1 != null && s1.equals(s2));
    }

    public static String getRelativePath(String parentPath, String childPath) {
        childPath = childPath.substring(parentPath.length(), childPath.length());
        childPath = formatRelativePath(childPath);
        return childPath;
    }
}
