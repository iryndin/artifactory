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
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "RepoType", propOrder = {"key", "description", "notes", "type", "includesPattern", "excludesPattern"},
        namespace = Descriptor.NS)
public abstract class RepoBaseDescriptor implements RepoDescriptor {

    @XmlID
    @XmlElement(required = true)
    private String key;

    @XmlElement(required = false)
    private String description;

    @XmlElement(required = false)
    private String notes;

    @XmlElement(defaultValue = "maven2", required = false)
    private RepoType type = RepoType.maven2;

    @XmlElement(defaultValue = "**/*", required = false)
    private String includesPattern = "**/*";

    @XmlElement(defaultValue = "", required = false)
    private String excludesPattern;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public RepoType getType() {
        return type;
    }

    public void setType(RepoType type) {
        this.type = type;
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

    public boolean identicalCache(RepoDescriptor oldDescriptor) {
        if (!(oldDescriptor instanceof RepoBaseDescriptor)) {
            return false;
        }
        RepoBaseDescriptor old = (RepoBaseDescriptor) oldDescriptor;
        if (!type.equals(old.type) ||
                !PathUtils.safeStringEquals(old.excludesPattern, excludesPattern) ||
                !PathUtils.safeStringEquals(old.includesPattern, includesPattern)
                ) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepoBaseDescriptor)) {
            return false;
        }
        RepoBaseDescriptor that = (RepoBaseDescriptor) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }

    public int compareTo(Object o) {
        if (o instanceof RepoDescriptor) {
            return key.compareTo(((RepoDescriptor) o).getKey());
        } else {
            return key.compareTo(o.toString());
        }
    }
}