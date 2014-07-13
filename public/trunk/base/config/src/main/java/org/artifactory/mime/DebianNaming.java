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

package org.artifactory.mime;

/**
 * @author Gidi Shabat
 */
public class DebianNaming {

    public final static String distribution = "deb.distribution";
    public final static String component = "deb.component";
    public final static String architecture = "deb.architecture";

    public static boolean isIndexFile(String fileName) {
        return isPackageIndex(fileName) || isReleaseIndex(fileName) || isContentIndex(fileName);
    }

    public static boolean isSupportedIndex(String fileName) {
        fileName = fileName.toLowerCase();
        switch (fileName) {
            case "release":
                return true;
            case "packages.gz":
                return true;
            case "packages.bz2":
                return true;
            case "packages":
                return true;
            default:
                return false;
        }
    }

    public static boolean isReleaseIndex(String fileName) {
        fileName = fileName.toLowerCase();
        switch (fileName) {
            case "release":
                return true;
            case "inrelease":
                return true;
            default:
                return false;
        }
    }

    public static boolean isPackageIndex(String fileName) {
        fileName = fileName.toLowerCase();
        switch (fileName) {
            case "packages":
                return true;
            case "packages.gz":
                return true;
            case "packages.bz":
                return true;
            case "packages.bz2":
                return true;
            default:
                return false;
        }
    }

    public static boolean isSigningFile(String fileName) {
        fileName = fileName.toLowerCase();
        if ("release.gpg".equals(fileName)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isInRelease(String fileName) {
        fileName = fileName.toLowerCase();
        if ("inrelease".equals(fileName)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isContentIndex(String fileName) {
        switch (fileName) {
            case "contents":
                return true;
            case "contents.gz":
                return true;
            case "contents.bz":
                return true;
            case "contents.bz2":
                return true;
            default:
                return false;
        }
    }

}
