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
package org.artifactory.repo;

import org.artifactory.resource.RepoResource;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.File;

@XmlType(name = "RepoType", propOrder = {"key", "description", "blackedOut",
        "handleReleases", "handleSnapshots", "includesPattern", "excludesPattern"})
public abstract class RepoBase implements Repo {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(RepoBase.class);

    public static final File TEMP_FOLDER =
            new File(System.getProperty("java.io.tmpdir"), "artifactory-uploads");

    private String key;
    private String description;
    private boolean handleReleases = true;
    private boolean handleSnapshots = true;
    private String includesPattern;
    private String excludesPattern;
    private boolean blackedOut;

    @XmlTransient
    private String[] includes;
    @XmlTransient
    private String[] excludes;

    @XmlElement(required = true)
    public String getKey() {
        return key;
    }

    @XmlElement(defaultValue = "default description", required = false)
    public String getDescription() {
        return description;
    }

    @XmlElement(defaultValue = "true", required = false)
    public boolean isHandleReleases() {
        return handleReleases;
    }

    public void setHandleReleases(boolean handleReleases) {
        this.handleReleases = handleReleases;
    }

    @XmlElement(defaultValue = "true", required = false)
    public boolean isHandleSnapshots() {
        return handleSnapshots;
    }

    public void setHandleSnapshots(boolean handleSnapshots) {
        this.handleSnapshots = handleSnapshots;
    }

    @XmlElement(defaultValue = "**/*", required = false)
    public String getIncludesPattern() {
        return includesPattern;
    }

    public void setIncludesPattern(String includesPattern) {
        this.includesPattern = includesPattern;
        if (!StringUtils.isEmpty(includesPattern)) {
            includes = StringUtils.split(includesPattern, ",");
            for (int i = 0; i < includes.length; i++) {
                String include = includes[i].replace('\\', '/');
                includes[i] = include;
            }
        }
    }

    @XmlElement(defaultValue = "", required = false)
    public String getExcludesPattern() {
        return excludesPattern;
    }

    public void setExcludesPattern(String excludesPattern) {
        this.excludesPattern = excludesPattern;
        if (!StringUtils.isEmpty(excludesPattern)) {
            excludes = StringUtils.split(excludesPattern, ",");
            for (int i = 0; i < excludes.length; i++) {
                String exclude = excludes[i].replace('\\', '/');
                excludes[i] = exclude;
            }
        }
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(defaultValue = "false", required = false)
    public boolean isBlackedOut() {
        return blackedOut;
    }

    public void setBlackedOut(boolean blackedOut) {
        this.blackedOut = blackedOut;
    }

    public abstract boolean isLocal();

    public boolean accept(String path) {
        if (excludes != null) {
            for (String exclude : excludes) {
                boolean match = SelectorUtils.match(exclude, path);
                if (match) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(this + " excludes pattern (" + excludesPattern
                                + ") rejected path '" + path + "'.");
                    }
                    return false;
                }
            }
        }
        if (includes != null) {
            for (String include : includes) {
                boolean match = SelectorUtils.match(include, path);
                if (match) {
                    return true;
                }
            }
        } else {
            return true;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(this + " includes pattern (" + includesPattern
                    + ") did not accept path '" + path + "'.");
        }
        return false;
    }

    /**
     * Gets the info from the physical repository (local or remote) without caching
     *
     * @param path
     * @return
     */
    protected abstract RepoResource retrieveInfo(String path);

    public String toString() {
        return key;
    }
}