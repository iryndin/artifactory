/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.JdkVersion;

import java.lang.reflect.Method;

/**
 * @author Yoav Landman
 */
public abstract class ThreadDumpUtils {

    private static final Logger log = LoggerFactory.getLogger(ThreadDumpUtils.class);

    public static void dumpThreads(StringBuilder msg) {
        try {
            if (JdkVersion.getMajorJavaVersion() >= JdkVersion.JAVA_16) {
                Class<?> dumperClass = Class.forName("org.artifactory.thread.ThreadDumper");
                Object dumper = dumperClass.newInstance();
                Method method = dumperClass.getDeclaredMethod("dumpThreads");
                CharSequence dump = (CharSequence) method.invoke(dumper);
                log.info("Printing locking debug information...");
                msg.append("\n").append(dump);
            }
        } catch (Throwable t) {
            log.info("Could not dump threads", t);
        }
    }
}