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
package org.artifactory.api.fs;

import org.artifactory.api.common.Info;
import org.artifactory.utils.PathUtils;

/**
 * @author freds
 * @date Oct 12, 2008
 */
public class ItemAdditionalInfo implements Info {
    private String createdBy;
    private String modifiedBy;
    /**
     * The last time the (cached) resource has been updated from it's remote location.
     */
    private long lastUpdated;

    public ItemAdditionalInfo() {
        this.lastUpdated = System.currentTimeMillis();
    }

    public ItemAdditionalInfo(ItemAdditionalInfo extension) {
        this.createdBy = extension.createdBy;
        this.modifiedBy = extension.modifiedBy;
        this.lastUpdated = extension.lastUpdated;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "ItemExtraInfo{" +
                "createdBy='" + createdBy + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public boolean isIdentical(ItemAdditionalInfo additionalInfo) {
        return this.lastUpdated == additionalInfo.lastUpdated &&
                PathUtils.safeStringEquals(this.modifiedBy, additionalInfo.modifiedBy) &&
                PathUtils.safeStringEquals(this.createdBy, additionalInfo.createdBy);
    }
}
