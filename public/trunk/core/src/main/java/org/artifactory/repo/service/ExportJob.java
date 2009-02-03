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
import org.artifactory.api.config.ExportSettings;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author freds
 * @date Nov 6, 2008
 */
public class ExportJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(ExportJob.class);

    public static final String REPO_KEY = "repoKey";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        StatusHolder status = null;
        try {
            JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
            String repoKey = (String) jobDataMap.get(REPO_KEY);
            ExportSettings settings = (ExportSettings) jobDataMap.get(ExportSettings.class.getName());
            status = (StatusHolder) jobDataMap.get(StatusHolder.class.getName());
            InternalRepositoryService service =
                    InternalContextHelper.get().beanForType(InternalRepositoryService.class);
            if (repoKey != null) {
                service.exportRepo(repoKey, settings, status);
            } else {
                service.exportTo(settings, status);
            }
        } catch (Exception e) {
            if (status != null) {
                status.setError("Error occured during export: " + e.getMessage(), e, log);
            } else {
                log.error("Error occured during export", e);
            }
        }
    }
}
