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

package org.artifactory.descriptor.cleanup;

import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.TaskDescriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Descriptor for the cleanup task
 *
 * @author Shay Yaakov
 */
@XmlType(name = "CleanupConfigType", propOrder = {"cronExp"}, namespace = Descriptor.NS)
public class CleanupConfigDescriptor implements TaskDescriptor {

    @XmlElement(required = true)
    private String cronExp;

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CleanupConfigDescriptor)) {
            return false;
        }

        CleanupConfigDescriptor that = (CleanupConfigDescriptor) o;

        if (!cronExp.equals(that.cronExp)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return cronExp.hashCode();
    }

    @Override
    public boolean sameTaskDefinition(TaskDescriptor otherDescriptor) {
        if (otherDescriptor == null || !(otherDescriptor instanceof CleanupConfigDescriptor)) {
            throw new IllegalArgumentException(
                    "Cannot compare cleanup config descriptor " + this + " with " + otherDescriptor);
        }
        CleanupConfigDescriptor cleanupConfigDescriptor = (CleanupConfigDescriptor) otherDescriptor;
        return StringUtils.equals(cleanupConfigDescriptor.cronExp, this.cronExp);
    }
}
