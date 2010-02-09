package org.artifactory.jcr;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ArtifactoryJcrConstants {
    String NT_ARTIFACTORY_FILE = "artifactory:file";
    String NT_ARTIFACTORY_FOLDER = "artifactory:folder";
    String NT_ARTIFACTORY_JAR = "artifactory:jar";
    String NT_ARTIFACTORY_XML_CONTENT = "artifactory:xmlcontent";
    String MIX_ARTIFACTORY_XML_AWARE = "artifactory:xmlAware";
    String MIX_ARTIFACTORY_POM_AWARE = "artifactory:pomAware";
    String MIX_ARTIFACTORY_REPO_AWARE = "artifactory:repoAware";
    String MIX_ARTIFACTORY_CACHEABLE = "artifactory:cacheable";
    String MIX_ARTIFACTORY_AUDITABLE = "artifactory:auditable";
    String PROP_ARTIFACTORY_NAME = "artifactory:name";
    String PROP_ARTIFACTORY_REPO_KEY = "artifactory:repoKey";
    String PROP_ARTIFACTORY_LAST_UPDATED = "artifactory:lastUpdated";
    String PROP_ARTIFACTORY_MODIFIED_BY = "artifactory:modifiedBy";
    String PROP_ARTIFACTORY_JAR_ENTRY = "artifactory:jarEntry";
    String ARTIFACTORY_XML = "artifactory:xml";
}
