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

import java.util.Arrays;

/**
 * @author Gidi Shabat
 */
public class DebianReleaseCalculationEvent extends DebianCalculationEvent {
    private String[] architectures;
    private String[] components;

    public DebianReleaseCalculationEvent(String distribution, String repoKey, RepoType repoType) {
        super(distribution, repoKey, repoType);
        components = new String[]{};
        architectures = new String[]{};
    }

    private DebianReleaseCalculationEvent(String distribution, String[] components, String[] architectures,
            String repoKey, RepoType repoType) {
        super(distribution, repoKey, repoType);
        this.components = components;
        this.architectures = architectures;
    }

    @Override
    public DebianCalculationEvent duplicateForRepo(String key, RepoType type) {
        return new DebianReleaseCalculationEvent(distribution, components, architectures, key, type);
    }


    public String[] getArchitectures() {
        return architectures;
    }

    public String[] getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return "Release [" + super.toString() + (architectures != null && architectures.length > 0 ?
                "architectures=" + Arrays.toString(architectures) + " " : "") +
                (components != null && components.length > 0 ? " components=" + Arrays.toString(components) + " " :
                        "") + "]";
    }

    @Override
    public int compareTo(DebianCalculationEvent o) {
        if (!(o instanceof DebianReleaseCalculationEvent)) {
            return -1;
        }
        if (repositoryType(this) != repositoryType(o)) {
            // local first
            return repositoryType(o) - repositoryType(this);
        }

        DebianReleaseCalculationEvent oPackage = (DebianReleaseCalculationEvent) o;
        String thisAll = repoKey + (distribution == null ? "" :
                distribution) + Arrays.toString(components) + Arrays.toString(architectures);
        String otherAll = oPackage.repoKey + (oPackage.distribution == null ? "" :
                oPackage.distribution) + Arrays.toString(oPackage.components) + Arrays.toString(oPackage.architectures);
        return thisAll.compareTo(otherAll);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DebianReleaseCalculationEvent that = (DebianReleaseCalculationEvent) o;
        if (repoKey != null ? !repoKey.equals(that.repoKey) : that.repoKey != null) {
            return false;
        }
        if (distribution != null ? !distribution.equals(that.distribution) : that.distribution != null) {
            return false;
        }
        if (!Arrays.equals(architectures, that.architectures)) {
            return false;
        }
        if (!Arrays.equals(components, that.components)) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = architectures != null ? Arrays.hashCode(architectures) : 0;
        result = 31 * result + (components != null ? Arrays.hashCode(components) : 0);
        result = 31 * result + (repoKey != null ? repoKey.hashCode() : 0);
        result = 31 * result + (distribution != null ? distribution.hashCode() : 0);
        return result;
    }

    private enum ReleaseDeptLevel {
        packageDept(2), distributionDept(1);
        private int dept;

        ReleaseDeptLevel(int level) {
            this.dept = level;
        }

        public int getDepth() {
            return dept;
        }
    }
}

