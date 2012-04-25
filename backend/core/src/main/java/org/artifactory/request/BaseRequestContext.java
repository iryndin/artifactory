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

package org.artifactory.request;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseRequestContext implements InternalRequestContext {
    private static final Pattern PATTERN_JAVA_AGENT = Pattern.compile("[Jj]ava/(.+)");

    protected final ArtifactoryRequest request;

    protected BaseRequestContext(@Nonnull ArtifactoryRequest request) {
        this.request = request;
    }

    @Override
    public String getServletContextUrl() {
        return request.getServletContextUrl();
    }

    @Override
    public boolean clientSupportsM3SnapshotVersions() {
        Request request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) {
            return true;
        }

        if (userAgent.startsWith("Wharf Ivy/") || userAgent.startsWith("Apache Ivy/")) {
            return false;
        }

        /**
         * Maven versions older that 2.0.9 (including) sent only the java version in the user agent; that's how we know
         * if the metadata should be filtered for compatibility
         */
        Matcher agentVersionMatcher = PATTERN_JAVA_AGENT.matcher(userAgent);
        return !agentVersionMatcher.matches();
    }

    @Override
    @Nonnull
    public ArtifactoryRequest getRequest() {
        return request;
    }
}
