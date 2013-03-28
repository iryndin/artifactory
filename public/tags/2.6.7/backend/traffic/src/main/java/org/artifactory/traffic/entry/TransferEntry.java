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

package org.artifactory.traffic.entry;

import org.artifactory.traffic.TrafficAction;

/**
 * Base entry for data transfer traffic entries.
 * <p/>
 * Example: 20090318162747|110|UPLOAD|libs-releases-local:antlr/antlr/2.7.7/antlr-2.7.7.pom|632
 * 20090319110249|7|UPLOAD|repo1-cache:antlr/antlr/2.7.6/antlr-2.7.6.jar|443432
 *
 * @author Yossi Shaul
 */
public abstract class TransferEntry extends TokenizedTrafficEntry {
    static final int COLUMNS_COUNT_SPEC = 5;

    private String repoPath;
    private long contentLength;

    /**
     * Parses the given textual entry and sets the object fields accordingly
     *
     * @param entry Textual entry
     */
    public TransferEntry(String entry) {
        super(entry);
        repoPath = tokens[3];
        contentLength = Long.parseLong(tokens[4]);
    }

    /**
     * Sets the given entry data in the relevant fields
     *
     * @param repoPath      Requested artifact repo path
     * @param contentLength Requested artifact size
     */
    public TransferEntry(String repoPath, long contentLength, long duration) {
        super(duration);
        this.repoPath = repoPath;
        this.contentLength = contentLength;
    }

    @Override
    protected void initTokens() {
        super.initTokens();
        tokens[3] = repoPath;
        tokens[4] = contentLength + "";
    }

    @Override
    public TrafficAction getAction() {
        return TrafficAction.UPLOAD;
    }

    @Override
    public int getColumnsCount() {
        return COLUMNS_COUNT_SPEC;
    }

    /**
     * Returns the requested artifact's repo path
     *
     * @return String - Repo path of requested artifact
     */
    public String getRepoPath() {
        return repoPath;
    }

    /**
     * Returns the requested artifact's size
     *
     * @return long - Size of requested artifact
     */
    public long getContentLength() {
        return contentLength;
    }
}
