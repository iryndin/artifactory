package org.artifactory.utils;

import org.apache.log4j.Logger;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An Ant based path matcher util class.
 *
 * @author Yossi Shaul
 */
public class PathMatcher {
    private final static Logger LOGGER = Logger.getLogger(PathMatcher.class);

    private static AntPathMatcher antPathMatcher = new AntPathMatcher();
    public static final List<String> DEFAULT_EXCLUDES = new ArrayList<String>(14) {
        {
            add("**/*~");
            add("**/#*#");
            add("**/.#*");
            add("**/%*%");
            add("**/._*");
            add("**/CVS");
            add("**/CVS/**");
            add("**/.cvsignore");
            add("**/SCCS");
            add("**/SCCS/**");
            add("**/vssver.scc");
            add("**/.svn");
            add("**/.svn/**");
            add("**/.DS_Store");
        }
    };

    public static String cleanPath(File file) {
        String path = file.getAbsolutePath();
        path = path.replace('\\', '/');
        if (path.startsWith("/") && path.length() > 1) {
            return path.substring(1);
        }
        return path;
    }

    public static boolean matches(File file, List<String> includes, List<String> excludes) {
        return matches(cleanPath(file), includes, excludes);
    }

    public static boolean matches(String path, List<String> includes, List<String> excludes) {
        if (excludes != null && !excludes.isEmpty()) {
            for (String exclude : excludes) {
                boolean match = antPathMatcher.match(exclude, path);
                if (match) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format(
                                "excludes pattern (%s) rejected path '%s'.", exclude, path));
                    }
                    return false;
                }
            }
        }
        if (includes != null && !includes.isEmpty()) {
            for (String include : includes) {
                boolean match = antPathMatcher.match(include, path);
                if (match) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    public static boolean isInDefaultExcludes(File file) {
        return matches(
                file,
                PathMatcher.DEFAULT_EXCLUDES, null);
    }
}
