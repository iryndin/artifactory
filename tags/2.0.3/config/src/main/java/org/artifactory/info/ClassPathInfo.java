package org.artifactory.info;

/**
 * An information group for all the java classpath properties
 *
 * @author Noam Tenne
 */
public class ClassPathInfo extends SystemInfoGroup {

    public ClassPathInfo() {
        super("sun.boot.class.path",
                "java.library.path",
                "java.endorsed.dirs",
                "java.ext.dirs",
                "java.class.path");
    }
}