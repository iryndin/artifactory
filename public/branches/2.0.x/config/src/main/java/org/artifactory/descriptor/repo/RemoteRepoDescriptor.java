/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.descriptor.repo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XmlType(name = "RemoteRepoBaseType", propOrder = {"type", "url", "offline", "hardFail",
        "storeArtifactsLocally", "retrievalCachePeriodSecs", "failedRetrievalCachePeriodSecs",
        "missedRetrievalCachePeriodSecs", "checksumPolicyType"})
public abstract class RemoteRepoDescriptor extends RealRepoDescriptor {

    @XmlElement(required = true)
    private String url;

    @XmlElement(defaultValue = "false", required = false)
    private boolean hardFail;

    @XmlElement(defaultValue = "false", required = false)
    private boolean offline;

    @XmlElement(defaultValue = "43200", required = false)
    private long retrievalCachePeriodSecs = 43200;//12hrs

    @XmlElement(defaultValue = "30", required = false)
    private long failedRetrievalCachePeriodSecs = 30;//30secs

    @XmlElement(defaultValue = "43200", required = false)
    private long missedRetrievalCachePeriodSecs = 43200;//12hrs

    @XmlElement(defaultValue = "true", required = false)
    protected boolean storeArtifactsLocally = true;

    @XmlElement(defaultValue = "maven2", required = false)
    private RemoteRepoType type = RemoteRepoType.maven2;

    @XmlElement(defaultValue = "generate-if-absent", required = false)
    private ChecksumPolicyType checksumPolicyType = ChecksumPolicyType.GEN_IF_ABSENT;

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

    public RemoteRepoType getType() {
        return type;
    }

    public void setType(RemoteRepoType type) {
        this.type = type;
    }

    public ChecksumPolicyType getChecksumPolicyType() {
        return checksumPolicyType;
    }

    public void setChecksumPolicyType(ChecksumPolicyType checksumPolicyType) {
        this.checksumPolicyType = checksumPolicyType;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isCache() {
        return false;
    }
}