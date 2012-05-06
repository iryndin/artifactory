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

package org.artifactory.build;

import org.jfrog.build.api.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A basic build run info holder
 *
 * @author Noam Y. Tenne
 */
public class BuildRunImpl implements BuildRun {

    private final String name;
    private final String number;
    private final String started;
    private final String releaseStatus;

    public BuildRunImpl(String name, String number, Date started) {
        this(name, number, new SimpleDateFormat(Build.STARTED_FORMAT).format(started));
    }

    /**
     * @param name    Build name
     * @param number  Build number
     * @param started Build started
     */
    public BuildRunImpl(String name, String number, String started) {
        this(name, number, started, null);
    }

    /**
     * @param name          Build name
     * @param number        Build number
     * @param started       Build started
     * @param releaseStatus Build release status
     */
    public BuildRunImpl(String name, String number, String started, String releaseStatus) {
        this.name = name;
        this.number = number;
        this.started = started;
        this.releaseStatus = releaseStatus;
    }

    /**
     * Returns the name of the build
     *
     * @return Build name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the number of the build
     *
     * @return Build number
     */
    @Override
    public String getNumber() {
        return number;
    }

    /**
     * Returns the starting time of the build
     *
     * @return Build start time
     */
    @Override
    public String getStarted() {
        return started;
    }

    /**
     * Returns a date representation of the build starting time
     *
     * @return Build started date
     * @throws java.text.ParseException
     */
    @Override
    public Date getStartedDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        try {
            return simpleDateFormat.parse(getStarted());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getReleaseStatus() {
        return releaseStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BuildRunImpl)) {
            return false;
        }

        BuildRunImpl run = (BuildRunImpl) o;

        if (!name.equals(run.name)) {
            return false;
        }
        if (!number.equals(run.number)) {
            return false;
        }
        if (started != null ? !started.equals(run.started) : run.started != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + number.hashCode();
        result = 31 * result + (started != null ? started.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BuildRun{" +
                "name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", started='" + started + '\'' +
                '}';
    }
}