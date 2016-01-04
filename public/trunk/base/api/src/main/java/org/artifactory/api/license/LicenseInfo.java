/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.license;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * This class represents a software license (e.g. apache, lgpl etc..) Each license is comprised of a name
 * (which is a must) a long name which describes it, the URL where to find the full license content, a
 * {@link java.util.regex.Pattern} which describes the license, and a boolean value whether this license is an approved
 * one to use.
 *
 * @author Tomer Cohen
 * @author Dan Feldman
 */
@XStreamAlias(LicenseInfo.ROOT)
public class LicenseInfo implements Serializable {

    public static final String NOT_FOUND = "Not Found";
    public static final String UNKNOWN = "Unknown";
    public static final String ROOT = "license";
    private static final String NOT_SEARCHED = "Not Searched";

    private String name = "";
    private String longName = "";
    private String url = "";
    private String regexp = "";
    private String comments = "";
    private boolean approved = false;
    private boolean unknown;
    private boolean validLicense;
    private boolean found;
    private boolean notFound;
    private boolean notSearched;



    public LicenseInfo(String name, String longName, String url) {
        this.name = name;
        this.longName = longName;
        this.url = url;
    }

    public LicenseInfo() {

    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValidLicense() {
        return !isUnknown() && isFound();
    }

    public boolean matchesLicense(String otherLicense) {
        //Blank - nothing to match
        if (StringUtils.isBlank(otherLicense)) {
            return false;
        }
        //Try exact match first
        if (otherLicense.equalsIgnoreCase(getName())) {
            return true;
        } else if (StringUtils.isNotBlank(regexp)) {
            return Pattern.compile(regexp, Pattern.CASE_INSENSITIVE).matcher(otherLicense).matches();
        }
        return false;
    }

    /**
     * No license info available
     */
    public static LicenseInfo createNotFound() {
        return new LicenseInfo(NOT_FOUND, "", "");
    }

    /**
     * Internal use, signifies license was not searched for by any LicenseLocatorStrategy
     */
    public static LicenseInfo createNotSearched() {
        return new LicenseInfo(NOT_SEARCHED, "", "");
    }

    public boolean isNotFound() {
        return name.equals(NOT_FOUND);
    }

    /**
     * NOTE: this is not the opposite of notFound - in order to be found a license's name must both be not equal to
     * NOT_FOUND AND NOT_SEARCHED (so this is a found license that was extracted)
     */
    public boolean isFound() {
        return !isNotFound() && !isNotSearched();
    }

    /**
     * License info exists, but wasn't matched to any existing license
     */
    public static LicenseInfo createUnknown(String name, String url) {
        return new LicenseInfo(UNKNOWN, name, url);
    }

    /**
     * Unknown license that no valid name was found for.
     * Mostly used for backwards compatibility with older versions that only saved 'unknown' as the name.
     */
    public static LicenseInfo createEmptyUnknown() {
        return new LicenseInfo(NOT_SEARCHED, "", "");
    }

    public boolean isUnknown() {
        return name.equals(UNKNOWN);
    }

    public boolean isNotSearched() {
        return name.equals(NOT_SEARCHED);
    }

    public static boolean isUnknownLicenseName(String licenseName) {
        return licenseName.equalsIgnoreCase(UNKNOWN);
    }

    public static boolean isNotFoundLicenseName(String licenseName) {
        return licenseName.equalsIgnoreCase(NOT_FOUND);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LicenseInfo that = (LicenseInfo) o;

        if(this.isNotFound() && that.isNotFound()) {
            return true;
        } else if (this.isNotSearched() && that.isNotSearched()) {
            return true;
        } else if (this.isUnknown() && that.isUnknown()) {
            return longName != null ? longName.equals(that.longName) : that.longName != null;
        } else {
            return name != null ? name.equals(that.name) : that.name != null;
        }
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = (longName != null ? (31 * result + longName.hashCode()) : result);
        return result;
    }

    @Override
    public String toString() {
        return "LicenseInfo" + "{name='" + name + '\'' + ", longName='" + longName + '\'' + ", url='" + url + '\'' + '}';
    }
}