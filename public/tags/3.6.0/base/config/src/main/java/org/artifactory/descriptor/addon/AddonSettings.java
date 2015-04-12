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

package org.artifactory.descriptor.addon;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlType;

/**
 * The descriptor of the mail server configuration
 */
@XmlType(name = "AddonsType", propOrder = {"showAddonsInfo", "showAddonsInfoCookie"},
        namespace = Descriptor.NS)
public class AddonSettings implements Descriptor {

    private boolean showAddonsInfo;
    private String showAddonsInfoCookie;

    public AddonSettings() {
        showAddonsInfo = true;
        showAddonsInfoCookie = System.currentTimeMillis() + "";
    }

    public boolean isShowAddonsInfo() {
        return showAddonsInfo;
    }

    public void setShowAddonsInfo(boolean showAddonsInfo) {
        this.showAddonsInfo = showAddonsInfo;
    }

    public String getShowAddonsInfoCookie() {
        return showAddonsInfoCookie;
    }

    public void setShowAddonsInfoCookie(String showAddonsInfoCookie) {
        this.showAddonsInfoCookie = showAddonsInfoCookie;
    }
}