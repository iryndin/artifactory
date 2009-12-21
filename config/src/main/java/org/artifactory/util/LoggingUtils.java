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

import org.slf4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class LoggingUtils {

    public static void warnOrDebug(Logger logger, String msg) {
        warnOrDebug(logger, msg, null);
    }

    public static void warnOrDebug(Logger logger, String msg, Throwable e) {
        if (logger.isDebugEnabled()) {
            if (e == null) {
                e = new RuntimeException();
            }
            logger.warn(msg + ".", e);
        } else {
            logger.warn(msg + (e != null ? (": " + e.getMessage() + ".") : "."));
        }
    }
}