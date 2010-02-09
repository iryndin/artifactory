/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.repo.service;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author freds
 * @date Nov 6, 2008
 */
public class ImportJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(ImportJob.class);

    public static final String REPO_KEY = "RepoKey";
    public static final String DELETE_REPO = "DeleteRepo";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) {
        StatusHolder status = null;
        try {
            JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
            String repoKey = (String) jobDataMap.get(REPO_KEY);
            RepoPath deleteRepo = (RepoPath) jobDataMap.get(DELETE_REPO);
            ImportSettings settings = (ImportSettings) jobDataMap.get(ImportSettings.class.getName());
            status = (StatusHolder) jobDataMap.get(StatusHolder.class.getName());
            InternalRepositoryService repositoryService =
                    InternalContextHelper.get().beanForType(InternalRepositoryService.class);
            if (repoKey != null) {
                if (repoKey.equals(PermissionTargetInfo.ANY_REPO)) {
                    repositoryService.importAll(settings, status);
                } else {
                    if (deleteRepo != null) {
                        status.setStatus("Fully removing repository '" + deleteRepo + "'.", log);
                        repositoryService.undeploy(deleteRepo);
                        status.setStatus("Repository '" + deleteRepo + "' fully deleted.", log);
                        try {
                            // Wait 2 seconds for the DB to delete the files..
                            // Bug in Jackrabbit/Derby:
                            // A lock could not be obtained within the time requested, state/code: 40XL1/30000
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            status.setError(e.getMessage(), e, log);
                        }
                    }
                    repositoryService.importRepo(repoKey, settings, status);
                }
            } else {
                repositoryService.importFrom(settings, status);
            }
        } catch (RuntimeException e) {
            if (status != null) {
                status.setError("Received Unhandled Exception", e, log);
            } else {
                log.error("Received Unhandled Exception", e);
            }
        }
    }
}