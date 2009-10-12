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

@XStreamAlias("ace")
public class AceInfo implements Info {
    private String principal;
    private boolean group;
    private int mask;

    public AceInfo() {
    }

    public AceInfo(String principal, boolean group, int mask) {
        this.principal = principal;
        this.group = group;
        this.mask = mask;
    }

    public AceInfo(AceInfo copy) {
        this(copy.principal, copy.group, copy.mask);
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public boolean canAdmin() {
        return (getMask() & ArtifactoryPermisssion.ADMIN.getMask()) > 0;
    }

    public void setAdmin(boolean admin) {
        if (admin) {
            setMask(getMask() | ArtifactoryPermisssion.ADMIN.getMask());
        } else {
            setMask(getMask() & ~ArtifactoryPermisssion.ADMIN.getMask());
        }
    }

    public boolean canDelete() {
        return (getMask() & ArtifactoryPermisssion.DELETE.getMask()) > 0;
    }

    public void setDelete(boolean delete) {
        if (delete) {
            setMask(getMask() | ArtifactoryPermisssion.DELETE.getMask());
        } else {
            setMask(getMask() & ~ArtifactoryPermisssion.DELETE.getMask());
        }
    }

    public boolean canDeploy() {
        return (getMask() & ArtifactoryPermisssion.DEPLOY.getMask()) > 0;
    }

    public void setDeploy(boolean deploy) {
        if (deploy) {
            setMask(getMask() | ArtifactoryPermisssion.DEPLOY.getMask());
        } else {
            setMask(getMask() & ~ArtifactoryPermisssion.DEPLOY.getMask());
        }
    }

    public boolean canRead() {
        return (getMask() & ArtifactoryPermisssion.READ.getMask()) > 0;
    }

    public void setRead(boolean read) {
        if (read) {
            setMask(getMask() | ArtifactoryPermisssion.READ.getMask());
        } else {
            setMask(getMask() & ~ArtifactoryPermisssion.READ.getMask());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AceInfo aceInfo = (AceInfo) o;
        return group == aceInfo.group &&
                !(principal != null ? !principal.equals(aceInfo.principal) :
                        aceInfo.principal != null);
    }

    @Override
    public int hashCode() {
        int result;
        result = (principal != null ? principal.hashCode() : 0);
        result = 31 * result + (group ? 1 : 0);
        return result;
    }
}