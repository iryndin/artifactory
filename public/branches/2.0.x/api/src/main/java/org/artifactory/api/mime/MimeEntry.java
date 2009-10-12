/**
 *
 */
package org.artifactory.api.mime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>A MIME type entry representing a MIME type as specified in <a href="http://tools.ietf.org/html/rfc2046">RFC
 * 2046</a> and an associated collection of file extensions.</p>
 *
 * @author Brennan Spies
 */
public class MimeEntry {
    private final String type, subType;
    private final List<String> fileExts;

    @SuppressWarnings("unchecked")
    public MimeEntry(String mime, String... exts) {
        //could use regex here to make sure only one '/'
        //and exclude other invalid chars
        if (mime.indexOf('/') == -1) {
            throw new IllegalArgumentException("MIME type '" + mime
                    + "' is not valid");
        }
        String[] parts = mime.split("/", 2);
        this.type = parts[0].toLowerCase();
        this.subType = parts[1].toLowerCase();
        this.fileExts = Arrays.asList(exts);
    }

    /**
     * Returns the type of the MIME as specified by "type/subtype".
     *
     * @return The type part of the MIME type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the "subtype" part of the MIME type.
     *
     * @return The subtype
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Adds a file extension to the collection of file extensions associated with the MIME type.
     *
     * @param fileExt The file extension
     */
    public void addFileExt(String fileExt) {
        fileExts.add(fileExt);
    }

    /**
     * Returns the file extensions associated with this MIME type.
     *
     * @return The file extensions
     */
    public Collection<String> getFileExts() {
        return fileExts;
    }

    /**
     * Returns the MIME type.
     *
     * @return The MIME type
     */
    public String getMimeType() {
        return type + "/" + subType;
    }

    /**
     * Shows entry as "mime-type ext1 ext2 ...".
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getMimeType()).append(' ');
        for (String ext : fileExts) {
            sb.append(' ').append(ext);
        }
        return sb.toString();
    }

    public String getDefaultExtension() {
        if (fileExts != null && !fileExts.isEmpty()) {
            return fileExts.get(0);
        }
        return null;
    }
}
