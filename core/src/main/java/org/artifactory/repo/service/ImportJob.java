/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.repo.service;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

/**
 * @author freds
 * @date Nov 6, 2008
 */
public class ImportJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(ImportJob.class);

    public static final String REPO_KEY = "repoKey";
    public static final String DELETE_REPO = "deleteRepo";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) {
        MultiStatusHolder status = null;
        try {
            JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
            String repoKey = (String) jobDataMap.get(REPO_KEY);
            boolean deleteRepo = (Boolean) jobDataMap.get(DELETE_REPO);
            ImportSettings settings = (ImportSettings) jobDataMap.get(ImportSettings.class.getName());
            status = settings.getStatusHolder();
            InternalRepositoryService repositoryService =
                    InternalContextHelper.get().beanForType(InternalRepositoryService.class);
            if (repoKey != null) {
                if (repoKey.equals(PermissionTargetInfo.ANY_REPO)) {
                    repositoryService.importAll(settings);
                } else {
                    if (deleteRepo) {
                        RepoPath deleteRepoPath = RepoPath.secureRepoPathForRepo(repoKey);
                        status.setStatus("Fully removing repository '" + deleteRepoPath + "'.", log);
                        try {
                            repositoryService.undeploy(deleteRepoPath);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                        status.setStatus("Repository '" + repoKey + "' fully deleted.", log);
                        try {
                            // Wait 2 seconds for the DB to delete the files..
                            // Bug in Jackrabbit/Derby:
                            // A lock could not be obtained within the time requested, state/code: 40XL1/30000
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            status.setError(e.getMessage(), e, log);
                        }
                    }
                    repositoryService.importRepo(repoKey, settings);
                }
            } else {
                repositoryService.importFrom(settings);
            }

            if (settings.isIndexMarkedArchives()) {
                InternalSearchService internalSearchService =
                        InternalContextHelper.get().beanForType(InternalSearchService.class);
                internalSearchService.indexMarkedArchives();
            }
        } catch (RuntimeException e) {
            if (status != null) {
                status.setError("Error occurred during import: " + e.getMessage(), e, log);
            } else {
                log.error("Error occurred during import", e);
            }
        }
    }

}