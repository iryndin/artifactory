/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.addon.debian;

import org.artifactory.addon.dpkgcommon.DpkgCalculationEvent;

import java.io.Serializable;

/**
 * @author Gidi Shabat
 */
public class DebianCalculationEvent extends DpkgCalculationEvent implements Comparable<DebianCalculationEvent>, Serializable {

    public static final String INDEX_ENTIRE_REPO = "reindex_entire_repo";

    private final String distribution;
    private final String component;
    private final String architecture;
    private final Layout layout;

    public DebianCalculationEvent(String distribution, String component, String architecture, String repoKey,
            Layout layout) {
        super(repoKey, false);
        this.distribution = distribution;
        this.component = component;
        this.architecture = architecture;
        this.layout = layout;
    }

    public static DebianCalculationEvent indexEntireRepoEvent(String repoKey, Layout layout) {
        return new DebianCalculationEvent(repoKey, layout);
    }

    /**
     * Constructs an event that will be interpreted as index entire repo
     */
    private DebianCalculationEvent(String repoKey, Layout layout) {
        super(repoKey, true);
        this.layout = layout;
        this.distribution = INDEX_ENTIRE_REPO;
        this.component = null;
        this.architecture = null;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getComponent() {
        return component;
    }

    public String getArchitecture() {
        return architecture;
    }

    public Layout getLayout() {
        return layout;
    }

    @Override
    public int compareTo(DebianCalculationEvent o) {
        int i = repoKey.compareTo(o.repoKey);
        if (i != 0) {
            return i;
        }
        if (distribution != null) {
            i = distribution.compareTo(o.distribution);
            if (i != 0) {
                return i;
            }
        }
        if (component != null) {
            i = component.compareTo(o.component);
            if (i != 0) {
                return i;
            }
        }
        if (architecture != null) {
            i = architecture.compareTo(o.architecture);
            if (i != 0) {
                return i;
            }
        }
        return i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DebianCalculationEvent that = (DebianCalculationEvent) o;
        if (distribution != null ? !distribution.equals(that.getDistribution()) : that.getDistribution() != null) {
            return false;
        }
        if (component != null ? !component.equals(that.getComponent()) : that.getComponent() != null) {
            return false;
        }
        return architecture != null ? architecture.equals(that.getArchitecture()) : that.getArchitecture() == null;
    }

    @Override
    public String toString() {
        return (repoKey != null ? "repoKey=" + repoKey + " " : "") +
                (distribution != null ? "distribution=" + distribution + " " : "") +
                (component != null ? "component=" + component + " " : "") +
                (architecture != null ? ", architecture=" + architecture + " " : "") + "]";
    }

    @Override
    public int hashCode() {
        int result = component != null ? component.hashCode() : 0;
        result = 31 * result + (architecture != null ? architecture.hashCode() : 0);
        result = 31 * result + (distribution != null ? distribution.hashCode() : 0);
        result = 31 * result + super.hashCode();
        return result;
    }

    public enum Layout implements Serializable{
        auto, trivial
    }
}
