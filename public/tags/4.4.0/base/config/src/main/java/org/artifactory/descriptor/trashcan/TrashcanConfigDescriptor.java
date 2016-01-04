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

package org.artifactory.descriptor.trashcan;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Configuration type for the global trashcan repository
 *
 * @author Shay Yaakov
 */
@XmlType(name = "TrashcanConfigType", propOrder = {"enabled", "allowPermDeletes", "retentionPeriodDays"},
        namespace = Descriptor.NS)
public class TrashcanConfigDescriptor implements Descriptor {

    public static final int DEFAULT_RETENTION_PERIOD_DAYS = 14;

    @XmlElement(defaultValue = "true")
    private boolean enabled = true;

    @XmlElement(defaultValue = "false")
    private boolean allowPermDeletes = false;

    @XmlElement(defaultValue = DEFAULT_RETENTION_PERIOD_DAYS + "")
    private int retentionPeriodDays = DEFAULT_RETENTION_PERIOD_DAYS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowPermDeletes() {
        return allowPermDeletes;
    }

    public void setAllowPermDeletes(boolean allowPermDeletes) {
        this.allowPermDeletes = allowPermDeletes;
    }

    public int getRetentionPeriodDays() {
        return retentionPeriodDays;
    }

    public void setRetentionPeriodDays(int retentionPeriodDays) {
        this.retentionPeriodDays = retentionPeriodDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrashcanConfigDescriptor that = (TrashcanConfigDescriptor) o;

        if (enabled != that.enabled) return false;
        if (allowPermDeletes != that.allowPermDeletes) return false;
        return retentionPeriodDays == that.retentionPeriodDays;

    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + (allowPermDeletes ? 1 : 0);
        result = 31 * result + retentionPeriodDays;
        return result;
    }
}
