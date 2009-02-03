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
package org.artifactory.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author yoavl
 */
public class ExecUtils {
    private static final Logger log = LoggerFactory.getLogger(ExecUtils.class);

    public static boolean execute(String cmd) {
        BufferedReader err = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            process.waitFor();
            InputStream errstr;
            errstr = process.getErrorStream();
            err = new BufferedReader(new InputStreamReader(errstr));
            if (errstr.available() > 0) {
                log.error("Problem executing command: '" + cmd + "':");
                String line;
                while ((line = err.readLine()) != null) {
                    log.error(line);
                }
                return false;
            }
            int exitValue = process.exitValue();
            return 0 == exitValue;
        } catch (Throwable t) {
            log.error("Received exception when executing command: '" + cmd + "'.", t);
            return false;
        } finally {
            IOUtils.closeQuietly(err);
        }
    }

    public static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.indexOf("windows") != -1;
    }
}