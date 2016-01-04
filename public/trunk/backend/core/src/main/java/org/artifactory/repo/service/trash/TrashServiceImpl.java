package org.artifactory.repo.service.trash;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.properties.PropertiesService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.StatusHolder;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesFactory;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.service.trash.prune.TrashcanPruner;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.binstore.service.InternalBinaryStore;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Shay Yaakov
 */
@Reloadable(beanClass = TrashService.class, initAfter = {InternalCentralConfigService.class})
public class TrashServiceImpl implements TrashService {
    private static final Logger log = LoggerFactory.getLogger(TrashServiceImpl.class);

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private InternalBinaryStore binaryStore;

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private PropertiesService propertiesService;

    public void init() {
        binaryStore.addGCListener(new TrashcanPruner());
    }

    @Override
    public void copyToTrash(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        if (TRASH_KEY.equals(repoKey)) {
            return;
        }

        LocalRepoDescriptor localRepoDescriptor = repoService.localOrCachedRepoDescriptorByKey(repoKey);
        if (localRepoDescriptor == null || localRepoDescriptor.isCache()) {
            return;
        }

        if (!configService.getDescriptor().getTrashcanConfig().isEnabled()) {
            return;
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication originalAuthentication = securityContext.getAuthentication();
        try {
            trashAsSystem(repoPath, originalAuthentication.getName());
        } finally {
            securityContext.setAuthentication(originalAuthentication);
        }
    }

    private void trashAsSystem(RepoPath repoPath, String deletedBy) {
        securityService.authenticateAsSystem();
        RepoPath trashPath = RepoPathFactory.create(TRASH_KEY, repoPath.getRepoKey() + "/" + repoPath.getPath());
        if (repoPath.isFile()) {
            log.debug("Trashing item '{}'", repoPath);
            Properties properties = buildDeletedItemProperties(repoPath, deletedBy);
            repoService.copy(repoPath, trashPath, false, true, true);
            repoService.setProperties(trashPath, properties);
        } else {
            log.debug("Overriding folder properties on trashed item '{}'", trashPath);
            if (repoService.exists(trashPath)) {
                repoService.setProperties(trashPath, buildDeletedItemProperties(repoPath, deletedBy));
            }
        }
    }

    @Override
    public BasicStatusHolder restore(RepoPath repoPath, String restoreRepo, String restorePath) {
        BasicStatusHolder status = new BasicStatusHolder();
        if (repoService.localRepositoryByKey(restoreRepo) == null) {
            status.warn("Restore repo '" + restoreRepo + "' doesn't exist", log);
            return status;
        }

        // First delete the unnecessary properties from the items in the trash
        propertiesService.deletePropertyRecursively(repoPath, PROP_TRASH_TIME, false);
        propertiesService.deletePropertyRecursively(repoPath, PROP_DELETED_BY, false);

        RepoPath restoreRepoPath = RepoPathFactory.create(restoreRepo, restorePath);
        if (rootRepoRestore(repoPath)) {
            repoService.getChildren(repoPath).stream()
                    .map(ItemInfo::getRepoPath)
                    .forEach(child -> restorePath(child, restoreRepoPath));
        } else {
            status = restorePath(repoPath, restoreRepoPath);
        }

        if (status.isError() || status.hasWarnings()) {
            return status;
        }

        return status;
    }

    private BasicStatusHolder restorePath(RepoPath repoPath, RepoPath restoreRepoPath) {
        return repoService.moveMultiTx(repoPath, restoreRepoPath, false, true, true);
    }

    private boolean rootRepoRestore(RepoPath repoPath) {
        RepoPath parent = repoPath.getParent();
        return parent != null && StringUtils.isBlank(parent.getPath());
    }

    @Override
    public StatusHolder empty() {
        return repoService.undeployMultiTransaction(InternalRepoPathFactory.repoRootPath(TRASH_KEY));
    }

    private Properties buildDeletedItemProperties(RepoPath repoPath, String deletedBy) {
        Properties properties = repoService.getProperties(repoPath);
        if (properties == null) {
            properties = PropertiesFactory.create();
        }
        String repoKey = repoPath.getRepoKey();
        properties.replaceValues(PROP_TRASH_TIME, Lists.newArrayList(String.valueOf(System.currentTimeMillis())));
        properties.replaceValues(PROP_DELETED_BY, Lists.newArrayList(deletedBy));
        properties.replaceValues(PROP_ORIGIN_REPO, Lists.newArrayList(repoKey));
        properties.replaceValues(PROP_ORIGIN_REPO_TYPE, Lists.newArrayList(repoService.repoDescriptorByKey(repoKey).getType().getType()));
        properties.replaceValues(PROP_ORIGIN_PATH, Lists.newArrayList(repoPath.getPath()));
        return properties;
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }
}
