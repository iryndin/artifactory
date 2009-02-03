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
        mimeTypesByExtension.put(".md5", new MimeType("text/plain"));
    }

    public static MimeType getMimeTypeByExtension(String extension) {
        return mimeTypesByExtension.get(extension);
    }

    public static MimeType getMimeTypeByPath(String path) {
        String extension = getExtension(path);
        return mimeTypesByExtension.get(extension);
    }

    /**
     * @param path
     * @return
     */
    static String getExtension(String path) {
        int dotPos = path.lastIndexOf('.');
        if (dotPos < 0) {
            return path;
        }
        return path.substring(dotPos);
    }

}