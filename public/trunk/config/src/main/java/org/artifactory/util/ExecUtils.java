/*
 * This file is part of Artifactory.
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

package org.artifactory.util;

import org.apache.commons.io.IOUtils;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

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
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        return osName.indexOf("windows") != -1;
    }
}