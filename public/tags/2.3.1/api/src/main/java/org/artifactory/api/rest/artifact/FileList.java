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

package org.artifactory.api.rest.artifact;

import java.io.Serializable;
import java.util.Set;

/**
 * File list REST command result object
 *
 * @author Noam Y. Tenne
 */
public class FileList implements Serializable {

    String uri;
    String created;
    Set<FileListElement> files;

    /**
     * Default constructor
     */
    public FileList() {
    }

    /**
     * Full constructor
     *
     * @param uri     URI of request sent by user
     * @param created The ISO8601 time the result was assembled
     * @param files   Set of files found
     */
    public FileList(String uri, String created, Set<FileListElement> files) {
        this.uri = uri;
        this.created = created;
        this.files = files;
    }

    /**
     * Returns the URI of the request
     *
     * @return Request URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the URI of the request
     *
     * @param uri Request URI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Returns the creation time of the result
     *
     * @return Result creation time
     */
    public String getCreated() {
        return created;
    }

    /**
     * Sets the creation time of the result
     *
     * @param created Result creation time
     */
    public void setCreated(String created) {
        this.created = created;
    }

    /**
     * Returns the set of files found
     *
     * @return Found file set
     */
    public Set<FileListElement> getFiles() {
        return files;
    }

    /**
     * Sets the set of files found
     *
     * @param files Found file set
     */
    public void setFiles(Set<FileListElement> files) {
        this.files = files;
    }
}