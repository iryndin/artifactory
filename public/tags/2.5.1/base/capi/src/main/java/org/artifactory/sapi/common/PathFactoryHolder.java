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

package org.artifactory.sapi.common;

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * Date: 8/4/11
 * Time: 11:39 AM
 *
 * @author Fred Simon
 */
public abstract class PathFactoryHolder {
    private static final Logger log = LoggerFactory.getLogger(PathFactoryHolder.class);

    private final static PathFactory DEFAULT_FACTORY;

    public static PathFactory get() {
        return DEFAULT_FACTORY;
    }

    static {
        PathFactory result = null;
        try {
            Class<?> cls =
                    Thread.currentThread().getContextClassLoader().loadClass(
                            "org.artifactory.jcr.factory.JcrPathFactory");
            result = (PathFactory) cls.newInstance();
        } catch (Exception e) {
            log.error("Could not create the default path factory object due to:" + e.getMessage(), e);
        }
        DEFAULT_FACTORY = result;
    }
}
