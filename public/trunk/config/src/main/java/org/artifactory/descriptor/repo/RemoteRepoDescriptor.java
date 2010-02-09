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
import org.artifactory.util.PathUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "RemoteRepoBaseType", propOrder = {"url", "offline", "hardFail", "storeArtifactsLocally",
        "fetchJarsEagerly", "fetchSourcesEagerly", "retrievalCachePeriodSecs", "failedRetrievalCachePeriodSecs",
        "missedRetrievalCachePeriodSecs", "checksumPolicyType", "unusedArtifactsCleanupEnabled",
        "unusedArtifactsCleanupPeriodHours", "shareConfiguration"},
        namespace = Descriptor.NS)
public abstract class RemoteRepoDescriptor extends RealRepoDescriptor {

    @XmlElement(required = true)
    private String url;

    @XmlElement(defaultValue = "false", required = false)
    private boolean hardFail;

    @XmlElement(defaultValue = "false", required = false)
    private boolean offline;

    @XmlElement(defaultValue = "true", required = false)
    protected boolean storeArtifactsLocally = true;

    @XmlElement(defaultValue = "false", required = false)
    protected boolean fetchJarsEagerly = false;

    @XmlElement(defaultValue = "false", required = false)
    protected boolean fetchSourcesEagerly = false;

    @XmlElement(defaultValue = "43200", required = false)
    private long retrievalCachePeriodSecs = 43200;//12hrs

    @XmlElement(defaultValue = "30", required = false)
    private long failedRetrievalCachePeriodSecs = 30;//30secs

    @XmlElement(defaultValue = "43200", required = false)
    private long missedRetrievalCachePeriodSecs = 43200;//12hrs

    @XmlElement(defaultValue = "generate-if-absent", required = false)
    private ChecksumPolicyType checksumPolicyType = ChecksumPolicyType.GEN_IF_ABSENT;

    @XmlElement(defaultValue = "false", required = false)
    protected boolean unusedArtifactsCleanupEnabled = false;

    @XmlElement(defaultValue = "0", required = false)
    private int unusedArtifactsCleanupPeriodHours = 0;

    @XmlElement(defaultValue = "false", required = false)
    protected boolean shareConfiguration = false;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isHardFail() {
        return hardFail;
    }

    public void setHardFail(boolean hardFail) {
        this.hardFail = hardFail;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public long getRetrievalCachePeriodSecs() {
        return retrievalCachePeriodSecs;
    }

    public void setRetrievalCachePeriodSecs(long retrievalCachePeriodSecs) {
        this.retrievalCachePeriodSecs = retrievalCachePeriodSecs;
    }

    public long getFailedRetrievalCachePeriodSecs() {
        return failedRetrievalCachePeriodSecs;
    }

    public void setFailedRetrievalCachePeriodSecs(long failedRetrievalCachePeriodSecs) {
        this.failedRetrievalCachePeriodSecs = failedRetrievalCachePeriodSecs;
    }

    public long getMissedRetrievalCachePeriodSecs() {
        return missedRetrievalCachePeriodSecs;
    }

    public void setMissedRetrievalCachePeriodSecs(long missedRetrievalCachePeriodSecs) {
        this.missedRetrievalCachePeriodSecs = missedRetrievalCachePeriodSecs;
    }

    public boolean isStoreArtifactsLocally() {
        return storeArtifactsLocally;
    }

    public void setStoreArtifactsLocally(boolean storeArtifactsLocally) {
        this.storeArtifactsLocally = storeArtifactsLocally;
    }

    public boolean isFetchJarsEagerly() {
        return fetchJarsEagerly;
    }

    public void setFetchJarsEagerly(boolean fetchJarsEagerly) {
        this.fetchJarsEagerly = fetchJarsEagerly;
    }

    public boolean isFetchSourcesEagerly() {
        return fetchSourcesEagerly;
    }

    public void setFetchSourcesEagerly(boolean fetchSourcesEagerly) {
        this.fetchSourcesEagerly = fetchSourcesEagerly;
    }

    public ChecksumPolicyType getChecksumPolicyType() {
        return checksumPolicyType;
    }

    public void setChecksumPolicyType(ChecksumPolicyType checksumPolicyType) {
        this.checksumPolicyType = checksumPolicyType;
    }

    public boolean isUnusedArtifactsCleanupEnabled() {
        return unusedArtifactsCleanupEnabled;
    }

    public void setUnusedArtifactsCleanupEnabled(boolean unusedArtifactsCleanupEnabled) {
        this.unusedArtifactsCleanupEnabled = unusedArtifactsCleanupEnabled;
    }

    public int getUnusedArtifactsCleanupPeriodHours() {
        return unusedArtifactsCleanupPeriodHours;
    }

    public void setUnusedArtifactsCleanupPeriodHours(int unusedArtifactsCleanupPeriodHours) {
        this.unusedArtifactsCleanupPeriodHours = unusedArtifactsCleanupPeriodHours;
    }

    public boolean isShareConfiguration() {
        return shareConfiguration;
    }

    public void setShareConfiguration(boolean shareConfiguration) {
        this.shareConfiguration = shareConfiguration;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isCache() {
        return false;
    }

    @Override
    public boolean identicalCache(RepoDescriptor oldDescriptor) {
        if (!super.identicalCache(oldDescriptor)) {
            return false;
        }
        RemoteRepoDescriptor old = (RemoteRepoDescriptor) oldDescriptor;
        if (!PathUtils.safeStringEquals(this.url,old.url) ||
                this.storeArtifactsLocally != old.storeArtifactsLocally ||
                this.retrievalCachePeriodSecs != old.retrievalCachePeriodSecs ||
                this.failedRetrievalCachePeriodSecs != old.failedRetrievalCachePeriodSecs ||
                this.missedRetrievalCachePeriodSecs != old.missedRetrievalCachePeriodSecs
                ) {
            return false;
        }
        return true;
    }
}