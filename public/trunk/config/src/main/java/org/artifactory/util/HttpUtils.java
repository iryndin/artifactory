package org.artifactory.util;

import org.artifactory.common.ConstantsValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yoavl
 */
public class HttpUtils {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);


    public static String getArtifactoryUserAgent() {
        //TODO: [by yl] Can probably cache this calc
        String artifactoryVersion = ConstantsValue.artifactoryVersion.getString();
        if (artifactoryVersion.startsWith("$") || artifactoryVersion.endsWith("SNAPSHOT")) {
            artifactoryVersion = "development";
        }
        return "Artifactory/" + artifactoryVersion;
    }
}
