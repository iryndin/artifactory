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
package org.artifactory.security;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.security.PermissionTargetInfo;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
@Node
public class PermissionTarget implements ObjectIdentity {

    public static final PermissionTarget ANY_PERMISSION_TARGET =
            new PermissionTarget(PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME,
                    PermissionTargetInfo.ANY_REPO, PermissionTargetInfo.ANY_PATH, null);

    @Field
    private String jcrName;

    @Field
    private String repoKey;

    private String name;
    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();

    public static PermissionTarget emptyTarget() {
        return new PermissionTarget("", "", "", "");
    }

    public PermissionTarget(
            String name, String repoKey, String includesPattern, String excludesPattern) {
        setName(name);
        this.repoKey = repoKey;
        setIncludesPattern(includesPattern);
        setExcludesPattern(excludesPattern);
    }

    public PermissionTarget(
            String name, String repoKey, List<String> includes, List<String> excludes) {
        setName(name);
        this.repoKey = repoKey;
        this.includes = includes;
        this.excludes = excludes;
    }

    public PermissionTarget() {
        //Also used by ocm
        this(PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME, PermissionTargetInfo.ANY_REPO,
                PermissionTargetInfo.ANY_PATH, null);
    }

    public PermissionTarget(PermissionTargetInfo descriptor) {
        this(descriptor.getName(), descriptor.getRepoKey(), descriptor.getIncludes(),
                descriptor.getExcludes());
    }

    public PermissionTargetInfo getDescriptor() {
        return new PermissionTargetInfo(name, repoKey, includes, excludes);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.jcrName = Text.escapeIllegalJcrChars(name);
    }

    @Field
    public String getIncludesPattern() {
        return StringUtils.collectionToCommaDelimitedString(includes);
    }

    public void setIncludesPattern(String includesPattern) {
        //Must be wrapped for ocm, otherwise uses an internal Arrays.List class
        this.includes = new ArrayList<String>(Arrays.asList(
                StringUtils.delimitedListToStringArray(includesPattern, ",", "\r\n\f ")));
    }

    @Field
    public String getExcludesPattern() {
        return StringUtils.collectionToCommaDelimitedString(excludes);
    }

    public void setExcludesPattern(String excludesPattern) {
        //Must be wrapped for ocm, otherwise uses an internal Arrays.List class
        this.excludes = new ArrayList<String>(Arrays.asList(
                StringUtils.delimitedListToStringArray(excludesPattern, ",", "\r\n\f ")));
    }

    public String getJcrName() {
        if (jcrName == null) {
            //Overcome the fact that xstream sets fields directly
            setName(this.name);
        }
        return jcrName;
    }

    public void setJcrName(String jcrName) {
        //Not to be used by clients
        this.jcrName = jcrName;
        this.name = Text.unescapeIllegalJcrChars(jcrName);
    }

    public String getIdentifier() {
        return name;
    }

    public Class getJavaType() {
        return PermissionTarget.class;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PermissionTarget target = (PermissionTarget) o;
        //Always use getter with omc (might be a cglib proxy)
        return name.equals(target.getName());
    }

    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
