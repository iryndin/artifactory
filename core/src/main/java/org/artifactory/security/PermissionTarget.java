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

package org.artifactory.security;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.security.PermissionTargetInfo;
import org.springframework.security.acls.model.ObjectIdentity;

import java.util.Arrays;
import java.util.List;

/**
 * Permission target holds a permissions for certain repositories paths.
 */
@Node
public class PermissionTarget implements ObjectIdentity {

    @Field(id = true)
    private String jcrName;

    private final PermissionTargetInfo info;

    /**
     * This dummy field is required to work around <a href="https://issues.apache.org/jira/browse/JCR-1928">ocm 1.5
     * bug</a> when using the annotation on the getters
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Collection(elementClassName = String.class, collectionConverter = MultiValueCollectionConverterImpl.class)
    private List<String> repoKeys;

    public PermissionTarget() {
        //Also used by ocm
        this(PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME,
                PermissionTargetInfo.ANY_REPO,
                PermissionTargetInfo.ANY_PATH, null);
    }

    public PermissionTarget(String name, String repoKeys, String includesPattern, String excludesPattern) {
        info = new PermissionTargetInfo(name, Arrays.asList(repoKeys), includesPattern, excludesPattern);
        setName(name);
    }

    public PermissionTarget(String name, List<String> repoKeys, String includesPattern, String excludesPattern) {
        info = new PermissionTargetInfo(name, repoKeys, includesPattern, excludesPattern);
        setName(name);
    }

    public PermissionTarget(String name, List<String> repoKeys, List<String> includes, List<String> excludes) {
        info = new PermissionTargetInfo(name, repoKeys, includes, excludes);
        setName(name);
    }

    public PermissionTarget(PermissionTargetInfo descriptor) {
        this(descriptor.getName(), descriptor.getRepoKeys(), descriptor.getIncludes(),
                descriptor.getExcludes());
    }

    public static PermissionTarget emptyTarget() {
        return new PermissionTarget("", "", "", "");
    }

    public PermissionTargetInfo getDescriptor() {
        return new PermissionTargetInfo(info);
    }

    //@Collection(elementClassName = String.class, collectionConverter = MultiValueCollectionConverterImpl.class)

    public List<String> getRepoKeys() {
        return info.getRepoKeys();
    }

    public void setRepoKeys(List<String> repoKeys) {
        info.setRepoKeys(repoKeys);
    }

    public List<String> getIncludes() {
        return info.getIncludes();
    }

    public void setIncludes(List<String> includes) {
        info.setIncludes(includes);
    }

    public List<String> getExcludes() {
        return info.getExcludes();
    }

    public void setExcludes(List<String> excludes) {
        info.setExcludes(excludes);
    }

    public String getName() {
        return info.getName();
    }

    public void setName(String name) {
        info.setName(name);
        this.jcrName = Text.escapeIllegalJcrChars(name);
    }

    @Field
    public String getIncludesPattern() {
        return info.getIncludesPattern();
    }

    public void setIncludesPattern(String includesPattern) {
        info.setIncludesPattern(includesPattern);
    }

    @Field
    public String getExcludesPattern() {
        return info.getExcludesPattern();
    }

    public void setExcludesPattern(String excludesPattern) {
        info.setExcludesPattern(excludesPattern);
    }

    public String getJcrName() {
        if (jcrName == null) {
            //Overcome the fact that xstream sets fields directly
            setName(info.getName());
        }
        return jcrName;
    }

    public void setJcrName(String jcrName) {
        //Not to be used by clients
        this.jcrName = jcrName;
        info.setName(Text.unescapeIllegalJcrChars(jcrName));
    }

    public String getIdentifier() {
        return info.getName();
    }

    public String getType() {
        return getClass().getName();
    }

    public Class getJavaType() {
        return PermissionTarget.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PermissionTarget target = (PermissionTarget) o;
        //Always use getter with omc (might be a cglib proxy)
        return info.getName().equals(target.getName());
    }

    @Override
    public int hashCode() {
        return info.getName().hashCode();
    }

    @Override
    public String toString() {
        return info.getName();
    }
}
