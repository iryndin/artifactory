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

package org.artifactory.log;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * @author Yoav Landman
 */
public class WrappingLogger implements Logger {

    private final String name;

    WrappingLogger(String name) {
        this.name = name;
    }

    public WrappingLogger(Class clazz) {
        this.name = clazz.getName();
    }

    public Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger(name);
    }

    @Override
    public String getName() {
        return getLogger().getName();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return getLogger().isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        getLogger().trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        getLogger().trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        getLogger().trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        getLogger().trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        getLogger().trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return getLogger().isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        getLogger().debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        getLogger().debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        getLogger().debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        getLogger().debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        getLogger().debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return getLogger().isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        getLogger().info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        getLogger().info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        getLogger().info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        getLogger().info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        getLogger().info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return getLogger().isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        getLogger().warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        getLogger().warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        getLogger().warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        getLogger().warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        getLogger().warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return getLogger().isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        getLogger().error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        getLogger().error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        getLogger().error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        getLogger().error(marker, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        getLogger().error(marker, msg, t);
    }

    @Override
    public String toString() {
        return getLogger().toString();
    }

    @Override
    public boolean isTraceEnabled() {
        return getLogger().isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        getLogger().trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        getLogger().trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        getLogger().trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object[] argArray) {
        getLogger().trace(format, argArray);
    }

    @Override
    public void trace(String msg, Throwable t) {
        getLogger().trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return getLogger().isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        getLogger().debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        getLogger().debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        getLogger().debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object[] argArray) {
        getLogger().debug(format, argArray);
    }

    @Override
    public void debug(String msg, Throwable t) {
        getLogger().debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return getLogger().isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        getLogger().info(msg);
    }

    @Override
    public void info(String format, Object arg1) {
        getLogger().info(format, arg1);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        getLogger().info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object[] argArray) {
        getLogger().info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t) {
        getLogger().info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return getLogger().isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        getLogger().warn(msg);
    }

    @Override
    public void warn(String format, Object arg1) {
        getLogger().warn(format, arg1);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        getLogger().warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object[] argArray) {
        getLogger().warn(format, argArray);
    }

    @Override
    public void warn(String msg, Throwable t) {
        getLogger().warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return getLogger().isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        getLogger().error(msg);
    }

    @Override
    public void error(String format, Object arg1) {
        getLogger().error(format, arg1);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        getLogger().error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object[] argArray) {
        getLogger().error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t) {
        getLogger().error(msg, t);
    }
}
