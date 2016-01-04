package org.artifactory.addon.opkg;

import org.artifactory.addon.Addon;
import org.artifactory.repo.RepoPath;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * @author Dan Feldman
 */
public interface OpkgAddon extends Addon {

    void recalculateEntireRepo(String repoKey, String password, boolean delayed, boolean writeProps);

    void calculateMetadata(Set<OpkgCalculationEvent> calculationRequests, @Nullable Collection<RepoPath> propertyWriterEntries, boolean delayed);
}
