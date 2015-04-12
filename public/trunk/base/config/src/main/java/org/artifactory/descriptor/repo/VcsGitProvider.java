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

package org.artifactory.descriptor.repo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Enum of the VCS Git repository type.
 *
 * @author Shay Yaakov
 */
@XmlEnum(String.class)
public enum VcsGitProvider {
    @XmlEnumValue("github")GITHUB("GitHub", "", "{0}/{1}/archive/{2}.{3}"),
    @XmlEnumValue("bitbucket")BITBUCKET("Bitbucket", "", "{0}/{1}/get/{2}.{3}"),
    @XmlEnumValue("stash")STASH("Stash", "scm/", "plugins/servlet/archive/projects/{0}/repos/{1}?at={2}&format={3}"),
    @XmlEnumValue("artifactory")ARTIFACTORY("Artifactory", "", "{0}/{1}/{2}?ext={3}"),
    @XmlEnumValue("custom")CUSTOM("Custom", "", "");

    private String prettyText;
    private String refsPrefix;
    private String downloadUrl;

    VcsGitProvider(String prettyText, String refsPrefix, String downloadUrl) {
        this.prettyText = prettyText;
        this.refsPrefix = refsPrefix;
        this.downloadUrl = downloadUrl;
    }

    public String getPrettyText() {
        return prettyText;
    }

    public String getRefsPrefix() {
        return refsPrefix;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}