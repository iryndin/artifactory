package org.artifactory.api.version;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * POJO used when calling home
 *
 * @author Shay Yaakov
 */
public class CallHomeRequest {

    public String product = "JFrog Artifactory";

    public String repository = "artifactory";

    @JsonProperty(value = "package")
    public String packageName = "jfrog-artifactory-generic";

    public String version;

    @JsonProperty(value = "artifactory_license_type")
    public String licenseType;

    @JsonProperty(value = "artifactory_license_oem")
    public String licenseOEM;

    @JsonProperty(value = "artifactory_license_expiration")
    public String licenseExpiration;

    public String dist = "unknown";

    public Environment environment = new Environment();

    public void setDist(String artdist) {
        if (StringUtils.isBlank(artdist)) {
            return;
        }

        this.dist = artdist;
        String repoSuffix = StringUtils.equals(this.licenseType, "oss") ? "" : "-pro";
        String pkgSuffix = StringUtils.equals(this.licenseType, "oss") ? "-oss" : "-pro";
        switch (artdist) {
            case "docker":
                this.repository = "registry";
                this.packageName = "artifactory:artifactory" + pkgSuffix;
                break;
            case "zip":
                this.repository = "artifactory" + repoSuffix;
                this.packageName = "jfrog-artifactory" + pkgSuffix + "-zip";
                break;
            case "rpm":
                this.repository = "artifactory" + repoSuffix + "-rpms";
                this.packageName = "jfrog-artifactory" + pkgSuffix + "-rpm";
                break;
            case "deb":
                this.repository = "artifactory" + repoSuffix + "-debs";
                this.packageName = "jfrog-artifactory" + pkgSuffix + "-deb";
                break;
        }
    }

    public static class Environment {
        @JsonProperty(value = "runtime_id")
        public String hostId;
        @JsonProperty(value = "user_id")
        public String licenseHash;
        public Attributes attributes = new Attributes();

        public static class Attributes {
            @JsonProperty(value = "os_name")
            public String osName;
            @JsonProperty(value = "os_arch")
            public String osArch;
            @JsonProperty(value = "java_version")
            public String javaVersion;
        }
    }
}
