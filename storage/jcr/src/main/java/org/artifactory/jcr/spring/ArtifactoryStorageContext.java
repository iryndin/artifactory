package org.artifactory.jcr.spring;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.schedule.TaskService;

/**
 * Date: 8/4/11 Time: 6:01 PM
 *
 * @author Fred Simon
 */
public interface ArtifactoryStorageContext extends ArtifactoryContext {
    JcrService getJcrService();

    JcrRepoService getJcrRepoService();

    MetadataDefinitionService getMetadataDefinitionService();

    JcrFsItemFactory storingRepositoryByKey(String repoKey);

    boolean isReady();

    TaskService getTaskService();
}
