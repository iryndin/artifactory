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

package org.artifactory.descriptor.security.ldap.group;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The descriptor which represents the LDAP group settings for integration between Artifactory and the specified LDAP
 * group.
 *
 * @author Tomer Cohen
 */
@XmlType(name = "LdapGroupSettingType",
        propOrder = {"name", "groupBaseDn", "groupNameAttribute", "groupMemberAttribute", "subTree", "filter",
                "descriptionAttribute", "strategy", "enabled"}, namespace = Descriptor.NS)
public class LdapGroupSetting implements Descriptor {

    private String name;

    private String groupBaseDn = "";

    private String groupNameAttribute = "cn";

    private String groupMemberAttribute;

    private boolean subTree;

    private String filter;

    private String descriptionAttribute;

    @XmlElement(defaultValue = "STATIC", required = false)
    private LdapGroupPopulatorStrategies strategy = LdapGroupPopulatorStrategies.STATIC;

    @XmlElement(defaultValue = "true")
    private boolean enabled = true;


    public LdapGroupSetting() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupBaseDn() {
        return groupBaseDn == null ? "" : groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public void setGroupMemberAttribute(String groupMemberAttribute) {
        this.groupMemberAttribute = groupMemberAttribute;
    }

    public boolean isSubTree() {
        return subTree;
    }

    public void setSubTree(boolean subTree) {
        this.subTree = subTree;
    }

    public String getDescriptionAttribute() {
        return descriptionAttribute;
    }

    public void setDescriptionAttribute(String descriptionAttribute) {
        this.descriptionAttribute = descriptionAttribute;
    }

    public LdapGroupPopulatorStrategies getStrategy() {
        return strategy;
    }

    public void setStrategy(LdapGroupPopulatorStrategies strategy) {
        this.strategy = strategy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LdapGroupSetting that = (LdapGroupSetting) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
