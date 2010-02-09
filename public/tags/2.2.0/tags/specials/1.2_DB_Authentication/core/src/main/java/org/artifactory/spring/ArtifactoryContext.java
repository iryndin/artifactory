package org.artifactory.spring;

import org.artifactory.backup.Backup;
import org.artifactory.maven.Maven;
import org.artifactory.repo.CentralConfig;
import org.artifactory.security.SecurityHelper;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ArtifactoryContext {
    CentralConfig getCentralConfig();

    SecurityHelper getSecurity();

    Maven getMaven();

    Backup getBackup();

    <T> T beanForType(Class<T> type);
}
