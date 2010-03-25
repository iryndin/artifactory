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

package org.artifactory.repo;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.DownloadService;
import org.artifactory.api.request.InternalArtifactoryRequest;
import org.artifactory.log.LoggerFactory;
import org.artifactory.request.InternalArtifactoryResponse;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * This job creates an internal request asking Artifactory to download certain resource(s).
 *
 * @author Yossi Shaul
 */
public class EagerResourcesFetchingJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(EagerResourcesFetchingJob.class);

    static final String PARAM_REPO_PATH = "repoPath";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        JobDataMap jobData = callbackContext.getJobDetail().getJobDataMap();
        RepoPath eagerRepoPath = (RepoPath) jobData.get(PARAM_REPO_PATH);

        InternalArtifactoryRequest internalRequest = new InternalArtifactoryRequest(eagerRepoPath);
        InternalArtifactoryResponse internalResponse = new InternalArtifactoryResponse();
        DownloadService downloadService = ContextHelper.get().beanForType(DownloadService.class);
        log.debug("Eager fetching path {}", eagerRepoPath);
        try {
            downloadService.process(internalRequest, internalResponse);
        } catch (IOException e) {
            // ignore - will be logged by the download service
        }
    }
}
