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
package org.artifactory.api.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;
import org.artifactory.utils.PathUtils;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("target")
public class PermissionTargetInfo implements Info {
    public static final String ANY_PERMISSION_TARGET_NAME = "Anything";
    public static final String ANY_REPO = "ANY";
    public static final String ANY_PATH = "**";

    private static final String DELIMITER = ",";

    private String name;
    private String repoKey;
    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();

    public PermissionTargetInfo() {
        this("", ANY_REPO);
    }

    public PermissionTargetInfo(String name, String repoKey) {
        this.name = name;
        this.repoKey = repoKey;
        this.includes.add(ANY_PATH);
    }

    public PermissionTargetInfo(String name, String repoKey,
            List<String> includes,
            List<String> excludes) {
        this.name = name;
        this.repoKey = repoKey;
        this.includes = includes;
        this.excludes = excludes;
    }

    public PermissionTargetInfo(PermissionTargetInfo copy) {
        this(
                copy.name, copy.repoKey,
                new ArrayList<String>(copy.includes),
                new ArrayList<String>(copy.excludes)
        );
    }

    public PermissionTargetInfo(String name, String repoKey, String includes, String excludes) {
        this(name, repoKey);
        setIncludesPattern(includes);
        setExcludesPattern(excludes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
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

        PermissionTargetInfo info = (PermissionTargetInfo) o;

        return !(name != null ? !name.equals(info.name) : info.name != null);
    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }
}