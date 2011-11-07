/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.model.xstream.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.security.MutablePermissionTargetInfo;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.util.PathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XStreamAlias("target")
public class PermissionTargetImpl implements MutablePermissionTargetInfo {

    private static final String DELIMITER = ",";

    private String name;
    private List<String> repoKeys = new ArrayList<String>();
    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();

    public PermissionTargetImpl() {
        this("", Arrays.asList(ANY_REPO));
    }

    public PermissionTargetImpl(String name) {
        this(name, Arrays.asList(ANY_REPO));
    }

    public PermissionTargetImpl(String name, List<String> repoKeys) {
        this.name = name;
        this.repoKeys = new ArrayList<String>(repoKeys);
        this.includes.add(ANY_PATH);
    }

    public PermissionTargetImpl(String name, List<String> repoKeys, List<String> includes, List<String> excludes) {
        this.name = name;
        this.repoKeys = repoKeys;
        this.includes = includes;
        this.excludes = excludes;
    }

    public PermissionTargetImpl(PermissionTargetInfo copy) {
        this(copy.getName(),
                new ArrayList<String>(copy.getRepoKeys()),
                new ArrayList<String>(copy.getIncludes()),
                new ArrayList<String>(copy.getExcludes())
        );
    }

    public PermissionTargetImpl(String name, List<String> repoKeys, String includes, String excludes) {
        this(name, repoKeys);
        setIncludesPattern(includes);
        setExcludesPattern(excludes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRepoKeys() {
        return repoKeys;
    }

    public void setRepoKeys(List<String> repoKeys) {
        this.repoKeys = repoKeys;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public String getIncludesPattern() {
        return PathUtils.collectionToDelimitedString(includes, DELIMITER);
    }

    public void setIncludesPattern(String includesPattern) {
        //Must be wrapped for ocm, otherwise uses an internal Arrays.List class
        this.includes = PathUtils.delimitedListToStringList(includesPattern, DELIMITER, "\r\n\f ");
    }

    public String getExcludesPattern() {
        return PathUtils.collectionToDelimitedString(excludes, DELIMITER);
    }

    public void setExcludesPattern(String excludesPattern) {
        //Must be wrapped for ocm, otherwise uses an internal Arrays.List class
        this.excludes = PathUtils.delimitedListToStringList(excludesPattern, DELIMITER, "\r\n\f ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PermissionTargetImpl info = (PermissionTargetImpl) o;

        return !(name != null ? !name.equals(info.name) : info.name != null);
    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }
}