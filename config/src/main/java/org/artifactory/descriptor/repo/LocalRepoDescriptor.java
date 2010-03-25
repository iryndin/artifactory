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

package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LocalRepoType", propOrder = {"snapshotVersionBehavior", "checksumPolicyType"},
        namespace = Descriptor.NS)
public class LocalRepoDescriptor extends RealRepoDescriptor {

    @XmlElement(defaultValue = "non-unique", required = false)
    private SnapshotVersionBehavior snapshotVersionBehavior = SnapshotVersionBehavior.NONUNIQUE;

    @XmlElement(name = "localRepoChecksumPolicyType", defaultValue = "client-checksums", required = false)
    private LocalRepoChecksumPolicyType checksumPolicyType = LocalRepoChecksumPolicyType.CLIENT;

    public SnapshotVersionBehavior getSnapshotVersionBehavior() {
        return snapshotVersionBehavior;
    }

    public void setSnapshotVersionBehavior(SnapshotVersionBehavior snapshotVersionBehavior) {
        this.snapshotVersionBehavior = snapshotVersionBehavior;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean isCache() {
        return false;
    }

    public LocalRepoChecksumPolicyType getChecksumPolicyType() {
        return checksumPolicyType;
    }

    public void setChecksumPolicyType(LocalRepoChecksumPolicyType checksumPolicyType) {
        this.checksumPolicyType = checksumPolicyType;
    }
}