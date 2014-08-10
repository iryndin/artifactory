/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.license;

import com.google.common.base.Predicate;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a software license (e.g. apache, lgpl etc..) Each license is comprised of a name (which is a
 * must) a long name which describes it, the URL where to find the full license content, a {@link
 * java.util.regex.Pattern} which describes the license, and a boolean value whether this license is an approved one to
 * use.
 *
 * @author Tomer Cohen
 */
@XStreamAlias(LicenseInfo.ROOT)
public class LicenseInfo implements Serializable {

    public static final LicenseInfo NOT_FOUND = createEmpty("Not Found");
    public static final LicenseInfo UNKNOWN = createEmpty("Unknown");
    public static final String ROOT = "license";
    public static final Predicate<LicenseInfo> NOT_VALID_LICENSE_PREDICATE = new Predicate<LicenseInfo>() {
        @Override
        public boolean apply(@Nonnull LicenseInfo input) {
            return !input.isValidLicense();
        }
    };
    private String name;
    private String longName;
    private String url;
    private String regexp;
    private String comments;
    private boolean approved;

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
        return !equals(UNKNOWN) && !equals(NOT_FOUND);
    }

    public boolean matchesLicense(String otherLicense) {
        if (StringUtils.isNotBlank(regexp) && StringUtils.isNotBlank(otherLicense)) {
            Pattern pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(otherLicense);
            if (matcher.matches()) {
                return true;
            }
        } else if (StringUtils.isNotBlank(otherLicense)) {
            // no regexp - try exact match
            if (otherLicense.equals(getName())) {
                return true;
            }
        }
        return false;
    }

    private static LicenseInfo createEmpty(String name) {
        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setName(name);
        licenseInfo.setLongName(name);
        return licenseInfo;
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

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LicenseInfo");
        sb.append("{name='").append(name).append('\'');
        sb.append(", longName='").append(longName).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
