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

package org.artifactory.api.build;

import org.jfrog.build.api.Build;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A basic build info holder
 *
 * @author Noam Y. Tenne
 */
public class BasicBuildInfo implements Serializable {

    private String name;
    private long number;
    private String started;

    /**
     * Main constructor
     *
     * @param name    Build name
     * @param number  Build number
     * @param started Build started
     */
    public BasicBuildInfo(String name, long number, String started) {
        this.name = name;
        this.number = number;
        this.started = started;
    }

    /**
     * Returns the name of the build
     *
     * @return Build name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of the build
     *
     * @return Build number
     */
    public long getNumber() {
        return number;
    }

    /**
     * Returns the starting time of the build
     *
     * @return Build start time
     */
    public String getStarted() {
        return started;
    }

    /**
     * Returns a date representation of the build starting time
     *
     * @return Build started date
     * @throws ParseException
     */
    public Date getStartedDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        try {
            return simpleDateFormat.parse(getStarted());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BasicBuildInfo)) {
            return false;
        }

        BasicBuildInfo that = (BasicBuildInfo) o;

        if (number != that.number) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (started != null ? !started.equals(that.started) : that.started != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (number ^ (number >>> 32));
        result = 31 * result + (started != null ? started.hashCode() : 0);
        return result;
    }
}