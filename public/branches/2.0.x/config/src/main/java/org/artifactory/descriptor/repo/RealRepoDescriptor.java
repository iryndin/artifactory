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

@XmlType(name = "RealRepoType", propOrder = {"blackedOut", "handleReleases", "handleSnapshots",
        "maxUniqueSnapshots", "includesPattern", "excludesPattern"})
public abstract class RealRepoDescriptor extends RepoBaseDescriptor {

    @XmlElement(defaultValue = "false", required = false)
    private boolean blackedOut;

    @XmlElement(defaultValue = "true", required = false)
    private boolean handleReleases = true;

    @XmlElement(defaultValue = "true", required = false)
    private boolean handleSnapshots = true;

    @XmlElement(defaultValue = "0", required = false)
    private int maxUniqueSnapshots;

    @XmlElement(defaultValue = "**/*", required = false)
    private String includesPattern = "**/*";

    @XmlElement(defaultValue = "", required = false)
    private String excludesPattern;

    public boolean isHandleReleases() {
        return handleReleases;
    }

    public void setHandleReleases(boolean handleReleases) {
        this.handleReleases = handleReleases;
    }

    public boolean isHandleSnapshots() {
        return handleSnapshots;
    }

    public void setHandleSnapshots(boolean handleSnapshots) {
        this.handleSnapshots = handleSnapshots;
    }

    public String getIncludesPattern() {
        return includesPattern;
    }

    public void setIncludesPattern(String includesPattern) {
        this.includesPattern = includesPattern;
    }

    public String getExcludesPattern() {
        return excludesPattern;
    }

    public void setExcludesPattern(String excludesPattern) {
        this.excludesPattern = excludesPattern;
    }

    public boolean isBlackedOut() {
        return blackedOut;
    }

    public void setBlackedOut(boolean blackedOut) {
        this.blackedOut = blackedOut;
    }

    public int getMaxUniqueSnapshots() {
        return maxUniqueSnapshots;
    }

    public void setMaxUniqueSnapshots(int maxUniqueSnapshots) {
        this.maxUniqueSnapshots = maxUniqueSnapshots;
    }

    public boolean isReal() {
        return true;
    }

    public abstract boolean isLocal();

    public abstract boolean isCache();
}