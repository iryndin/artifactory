/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.rest.replication;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Holds the info about the latest replication that annotates a replication root item
 *
 * @author Noam Y. Tenne
 */
public class ReplicationStatus implements Serializable {

    private String status;
    private String lastCompleted;

    public ReplicationStatus() {
    }

    public ReplicationStatus(String status, @Nullable String lastCompleted) {
        this.status = status;
        this.lastCompleted = lastCompleted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Nullable
    public String getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(@Nullable String lastCompleted) {
        this.lastCompleted = lastCompleted;
    }
}
