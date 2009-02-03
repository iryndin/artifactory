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

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class WagonManagerTempArtifactsCleaner extends TimerTask {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(WagonManagerTempArtifactsCleaner.class);

    @SuppressWarnings({"unchecked"})
    public void run() {
        try {
            File temp = File.createTempFile("maven-artifact", null);
            File tempDir = temp.getParentFile();
            List<File> files = FileUtils.getFiles(tempDir, "maven-artifact*.tmp", null);
            int count = -1; //Exclude the test temp file
            for (File file : files) {
                boolean res = file.delete();
                if (res) {
                    count++;
                }
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("WagonManager temp artifacts cleaner deleted " + count + " files.");
            }
        } catch (IOException e) {
            LOGGER.error("WagonManager temp artifacts cleaner failed.", e);
        }
    }
}
