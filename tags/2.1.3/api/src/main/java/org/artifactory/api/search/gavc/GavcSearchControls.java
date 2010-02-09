/*
 * This file is part of Artifactory.
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

package org.artifactory.api.search.gavc;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.SearchControlsBase;

/**
 * Holds the GAVC search parameters
 *
 * @author Noam Tenne
 */
public class GavcSearchControls extends SearchControlsBase {

    private String groupId;
    private boolean groupExactMatch = true;
    private String artifactId;
    private boolean artifactExactMatch = true;
    private String version;
    private boolean versionExactMatch = true;
    private String classifier;

    /**
     * Default constructor
     */
    public GavcSearchControls() {
    }

    /**
     * Copy constructor
     *
     * @param gavcSearchControls Controls to copy
     */
    public GavcSearchControls(GavcSearchControls gavcSearchControls) {
        this.groupId = gavcSearchControls.groupId;
        this.groupExactMatch = gavcSearchControls.groupExactMatch;
        this.artifactId = gavcSearchControls.artifactId;
        this.artifactExactMatch = gavcSearchControls.artifactExactMatch;
        this.version = gavcSearchControls.version;
        this.versionExactMatch = gavcSearchControls.versionExactMatch;
        this.classifier = gavcSearchControls.classifier;
        this.selectedRepoForSearch = gavcSearchControls.selectedRepoForSearch;
        setLimitSearchResults(gavcSearchControls.isLimitSearchResults());
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isGroupExactMatch() {
        return groupExactMatch;
    }

    public void setGroupExactMatch(boolean groupExactMatch) {
        this.groupExactMatch = groupExactMatch;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public boolean isArtifactExactMatch() {
        return artifactExactMatch;
    }

    public void setArtifactExactMatch(boolean artifactExactMatch) {
        this.artifactExactMatch = artifactExactMatch;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isVersionExactMatch() {
        return versionExactMatch;
    }

    public void setVersionExactMatch(boolean versionExactMatch) {
        this.versionExactMatch = versionExactMatch;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public boolean isEmpty() {
        return (isEmpty(groupId) && isEmpty(artifactId) && isEmpty(version)) && isEmpty(classifier);
    }

    public String getSearchExpression() {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(groupId)) {
            sb.append(groupId).append("-");
        }
        if (!isEmpty(artifactId)) {
            sb.append(artifactId).append("-");
        }
        if (!isEmpty(version)) {
            sb.append(version).append("-");
        }
        if (!isEmpty(classifier)) {
            sb.append(classifier);
        }

        String expression = sb.toString();
        if (expression.endsWith("-")) {
            expression = expression.substring(0, (expression.length() - 1));
        }

        return expression;
    }

    private boolean isEmpty(String field) {
        return StringUtils.isEmpty(field);
    }
}