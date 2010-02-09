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

package org.artifactory.maven;

import org.apache.commons.lang.time.DateUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.codehaus.plexus.util.FileUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author yoavl
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
