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

package org.artifactory.api.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * User: freds Date: Aug 5, 2008 Time: 9:30:26 PM
 */
@XStreamAlias("artifactoryVersion")
public class VersionInfo implements Serializable, Comparable<VersionInfo> {

    private static final String SNAPSHOT = "-SNAPSHOT";
    private static final Logger log = LoggerFactory.getLogger(VersionInfo.class);

    private String version;
    private String revision;

    /**
     * Serialization .ctr
     */
    public VersionInfo() {
    }

    public VersionInfo(String version, String revision) {
        this.version = version;
        this.revision = revision;
    }

    public VersionInfo(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "version='" + version + '\'' +
                ", revision='" + revision + '\'' +
                '}';
    }

    /**
     * Performs version compare
     *
     * @param other {@link VersionInfo} to compare against this instance
     *
     * @exception  NumberFormatException if the {@code Version}
     *             does not contain a parsable {@code int}.
     *
     * @return int (-1/0/1)
     */
    @Override
    public int compareTo(VersionInfo other) {
        if (other.getVersion().endsWith(SNAPSHOT)) {
            log.debug("Found development {} version, assuming it grader than any other version ...", other);
            return 1;
        }

        String[] arrLeft = other.getVersion().split("\\.");
        String[] arrRight = this.getVersion().split("\\.");

        // TODO: Should we support revision as well?

        int i=0;
        while(i<arrLeft.length || i<arrRight.length){
            if(i<arrLeft.length && i<arrRight.length){
                if(Integer.parseInt(arrLeft[i]) < Integer.parseInt(arrRight[i])){
                    return -1;
                }else if(Integer.parseInt(arrLeft[i]) > Integer.parseInt(arrRight[i])){
                    return 1;
                }
            } else if(i<arrLeft.length){
                if(Integer.parseInt(arrLeft[i]) != 0){
                    return 1;
                }
            } else if(i<arrRight.length){
                if(Integer.parseInt(arrRight[i]) != 0){
                    return -1;
                }
            }

            i++;
        }

        return 0;
    }

    /**
     * Defines equality of VersionInfo
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VersionInfo)) return false;

        VersionInfo that = (VersionInfo)other;
        return this.getVersion().equals(that.getVersion()) &&
                this.getRevision().equals(that.getRevision());
    }
}
