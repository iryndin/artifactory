/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.debian;

/**
 * @author Gidi Shabat
 */
public abstract class DebianCalculationEvent implements Comparable<DebianCalculationEvent> {
    protected String passphrase;
    protected final String distribution;
    protected final String repoKey;
    protected final RepoType repoType;
    private final long timestamp;

    public DebianCalculationEvent(String distribution, String repoKey, RepoType repoType) {
        this.distribution = distribution;
        this.repoKey = repoKey;
        this.repoType = repoType;
        this.timestamp = System.nanoTime();
    }

    public void setPassphrase(String password) {
        this.passphrase = password;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getRepoKey() {
        return repoKey;
    }

    @Override
    public String toString() {
        return (repoKey != null ? "repoKey=" + repoKey + " " : "") +
                (distribution != null ? "distribution=" + distribution + " " : "");
    }

    public long timestamp() {
        return timestamp;
    }

    public enum RepoType {
        local, remote, virtual
    }
}
