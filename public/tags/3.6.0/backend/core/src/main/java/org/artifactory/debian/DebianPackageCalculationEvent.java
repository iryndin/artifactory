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

package org.artifactory.debian;

/**
 * @author Gidi Shabat
 */
public class DebianPackageCalculationEvent extends DebianCalculationEvent {

    private final String component;
    private final String architecture;
    private String packageType;

    public DebianPackageCalculationEvent(String distribution, String component, String packageType, String architecture,
            String repoKey,
            RepoType repoType) {
        super(distribution, repoKey, repoType);
        this.component = component;
        this.packageType = packageType;
        this.architecture = architecture;
    }

    @Override
    public int compareTo(DebianCalculationEvent o) {
        DebianPackageCalculationEvent oPackage = (DebianPackageCalculationEvent) o;
        int i = repoKey.compareTo(oPackage.repoKey);
        if (i != 0) {
            return i;
        }
        if (distribution != null) {
            i = distribution.compareTo(oPackage.distribution);
            if (i != 0) {
                return i;
            }
        }
        if (component != null) {
            i = component.compareTo(oPackage.component);
            if (i != 0) {
                return i;
            }
        }
        if (architecture != null) {
            i = architecture.compareTo(oPackage.architecture);
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

        DebianPackageCalculationEvent that = (DebianPackageCalculationEvent) o;
        if (repoKey != null ? !repoKey.equals(that.repoKey) : that.repoKey != null) {
            return false;
        }
        if (distribution != null ? !distribution.equals(that.distribution) : that.distribution != null) {
            return false;
        }
        if (architecture != null ? !architecture.equals(that.architecture) : that.architecture != null) {
            return false;
        }
        if (component != null ? !component.equals(that.component) : that.component != null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Package [" + super.toString() + " " +
                (component != null ? "component=" + component + " " : "") +
                (architecture != null ? ", architecture=" + architecture + " " : "") + "]";
    }

    @Override
    public int hashCode() {
        int result = component != null ? component.hashCode() : 0;
        result = 31 * result + (architecture != null ? architecture.hashCode() : 0);
        result = 31 * result + (repoKey != null ? repoKey.hashCode() : 0);
        result = 31 * result + (distribution != null ? distribution.hashCode() : 0);
        return result;
    }

    public String getComponent() {
        return component;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getPackageType() {
        return packageType;
    }
}