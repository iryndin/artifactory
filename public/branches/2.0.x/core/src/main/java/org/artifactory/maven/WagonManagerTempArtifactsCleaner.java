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
package org.artifactory.maven;

import org.apache.commons.lang.time.DateUtils;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.codehaus.plexus.util.FileUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class WagonManagerTempArtifactsCleaner extends QuartzCommand {
    private static final Logger log =
            LoggerFactory.getLogger(WagonManagerTempArtifactsCleaner.class);
    private static final long MIN_AGE = DateUtils.MILLIS_PER_MINUTE * 5;

    @SuppressWarnings({"unchecked"})
    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            File temp = File.createTempFile("maven-artifact", null);
            File tempDir = temp.getParentFile();
            List<File> files = FileUtils.getFiles(tempDir, "maven-artifact*.tmp", null);
            int count = 0;
            for (File file : files) {
                //Only delete files older than 5 mins to avoid interfering with active deployments
                if (System.currentTimeMillis() - file.lastModified() > MIN_AGE) {
                    boolean res = file.delete();
                    if (res && file.equals(temp)) {
                        count++;
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("WagonManager temp artifacts cleaner deleted " + count + " file(s).");
            }
        } catch (IOException e) {
            log.error("WagonManager temp artifacts cleaner failed.", e);
        }
    }
}
