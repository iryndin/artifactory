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

package org.artifactory.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author Yoav Landman
 */
public final class LoggerFactory {

    public static Logger getLogger(String name) {
        return org.slf4j.LoggerFactory.getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return org.slf4j.LoggerFactory.getLogger(clazz);
    }

    public static ILoggerFactory getILoggerFactory() {
        return org.slf4j.LoggerFactory.getILoggerFactory();
    }
}