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

package org.artifactory.repo.jcr;

import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.LocalRepoChecksumPolicy;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.snapshot.MavenSnapshotVersionAdapter;
import org.artifactory.repo.snapshot.SnapshotVersionAdapterBase;

public class JcrLocalRepo extends JcrRepoBase<LocalRepoDescriptor> {

    // For local non-cache repositories use the special local repo checksum policy
    private final LocalRepoChecksumPolicy checksumPolicy = new LocalRepoChecksumPolicy();
    private final MavenSnapshotVersionAdapter mavenSnapshotVersionAdapter;

    public JcrLocalRepo(InternalRepositoryService repositoryService, LocalRepoDescriptor descriptor,
            JcrLocalRepo oldRepo) {
        super(repositoryService, oldRepo != null ? oldRepo.getStorageMixin() : null);
        setDescriptor(descriptor);
        checksumPolicy.setPolicyType(descriptor.getChecksumPolicyType());
        SnapshotVersionBehavior snapshotVersionBehavior = descriptor.getSnapshotVersionBehavior();
        mavenSnapshotVersionAdapter = SnapshotVersionAdapterBase.getByType(snapshotVersionBehavior);

    }

    @Override
    public boolean isSuppressPomConsistencyChecks() {
        return getDescriptor().isSuppressPomConsistencyChecks();
    }

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    @Override
    public MavenSnapshotVersionAdapter getMavenSnapshotVersionAdapter() {
        return mavenSnapshotVersionAdapter;
    }

    @Override
    public void onCreate(JcrFsItem fsItem) {
        // nothing special
    }
}