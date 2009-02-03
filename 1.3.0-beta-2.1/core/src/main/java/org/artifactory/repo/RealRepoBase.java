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

import org.artifactory.fs.FsItemMetadata;
import org.artifactory.maven.MavenUtils;
import org.artifactory.resource.ArtifactResource;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.File;

@XmlType(name = "RealRepoType", propOrder = {"blackedOut", "handleReleases", "handleSnapshots",
        "maxUniqueSnapshots", "includesPattern", "excludesPattern"})
public abstract class RealRepoBase extends RepoBase implements RealRepo {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(RealRepoBase.class);

    public static final File TEMP_FOLDER =
            new File(System.getProperty("java.io.tmpdir"), "artifactory-uploads");

    private boolean blackedOut;
    private boolean handleReleases = true;
    private boolean handleSnapshots = true;
    private int maxUniqueSnapshots;
    private String includesPattern;
    private String excludesPattern;

    @XmlTransient
    private String[] includes;
    @XmlTransient
    private String[] excludes;

    public boolean isReal() {
        return true;
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

    @XmlElement(defaultValue = "false", required = false)
    public boolean isBlackedOut() {
        return blackedOut;
    }

    @XmlElement(defaultValue = "0", required = false)
    public int getMaxUniqueSnapshots() {
        return maxUniqueSnapshots;
    }

    public void setBlackedOut(boolean blackedOut) {
        this.blackedOut = blackedOut;
    }

    public void setMaxUniqueSnapshots(int maxUniqueSnapshots) {
        this.maxUniqueSnapshots = maxUniqueSnapshots;
    }

    public boolean accepts(String path) {
        String toCheck = path;
        // For artifactory metadata the pattern apply to the object it represents
        if (path.endsWith(FsItemMetadata.SUFFIX)) {
            toCheck = path.substring(0, path.length() - FsItemMetadata.SUFFIX.length() - 1);
        }
        if (excludes != null) {
            for (String exclude : excludes) {
                boolean match = SelectorUtils.match(exclude, toCheck);
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
                if (toCheck.length() < include.length()) {
                    if (include.startsWith(toCheck)) {
                        return true;
                    }
                }
                boolean match = SelectorUtils.match(include, toCheck);
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

    public boolean handles(String path) {
        if (MavenUtils.isXml(path) || MavenUtils.isChecksum(path)) {
            return true;
        }
        boolean snapshot = MavenUtils.isSnapshot(path);
        if (snapshot && !handleSnapshots) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this + " rejected '" + path + "': not handling snapshots.");
            }
            return false;
        } else if (!snapshot && !handleReleases) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this + " rejected '" + path + "': not handling releases.");
            }
            return false;
        }
        return true;
    }

    public boolean handles(ArtifactResource res) {
        return handles(res.getPath());
    }
}