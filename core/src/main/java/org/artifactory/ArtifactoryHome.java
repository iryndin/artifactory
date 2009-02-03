package org.artifactory;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryHome {
    public static String SYS_PROP = "artifactory.home";

    public static String path() {
        return System.getProperty(SYS_PROP);
    }

    public static File file() {
        return new File(path());
    }

    public static void create() {
        File logs = new File(file(), "logs");
        logs.mkdirs();
    }
}
