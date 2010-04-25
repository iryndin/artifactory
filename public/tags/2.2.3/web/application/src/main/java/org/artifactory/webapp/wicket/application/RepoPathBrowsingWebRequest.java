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

package org.artifactory.webapp.wicket.application;

import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.servlet.RequestUtils;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class RepoPathBrowsingWebRequest extends ServletWebRequest {
    private static final Logger log = LoggerFactory.getLogger(RepoPathBrowsingWebRequest.class);

    private int depthRelativeToWicketHandler = -1;

    public RepoPathBrowsingWebRequest(WebRequest request) {
        super(request.getHttpServletRequest());
    }

    @Override
    public String getRelativePathPrefixToContextRoot() {
        String prefix = getPrefix();
        if (prefix == null) {
            return super.getRelativePathPrefixToContextRoot();
        }
        return prefix;
    }

    @Override
    public String getRelativePathPrefixToWicketHandler() {
        String prefix = getPrefix();
        if (prefix == null) {
            return super.getRelativePathPrefixToWicketHandler();
        }
        String path = getURL();
        int begin = 0;
        if (path.startsWith("/")) {
            begin = 1;
        }
        int end = path.indexOf('/', begin);
        if (end > 0) {
            prefix += path.substring(begin, end);
        } else {
            prefix += path;
        }
        prefix += "/";
        return prefix;
    }

    @Override
    public int getDepthRelativeToWicketHandler() {
        if (depthRelativeToWicketHandler != -1) {
            return depthRelativeToWicketHandler;
        }
        //Initialize
        RepoPath repoPath = RequestUtils.getRepoPath(this);
        if (repoPath != null) {
            String path = repoPath.getPath();
            if (StringUtils.hasLength(path)) {
                depthRelativeToWicketHandler = new StringTokenizer(path, "/").countTokens();
            } else {
                depthRelativeToWicketHandler = 0;
            }
        } else {
            depthRelativeToWicketHandler = super.getDepthRelativeToWicketHandler();
        }
        return depthRelativeToWicketHandler;
    }

    private String getPrefix() {
        RepoPath repoPath = RequestUtils.getRepoPath(this);
        if (repoPath != null) {
            String path = repoPath.getPath();
            StringBuilder prefix = new StringBuilder();
            if (StringUtils.hasLength(path)) {
                int nesting = new StringTokenizer(path, "/").countTokens() + 1;
                while (nesting > 0) {
                    prefix.append("../");
                    nesting--;
                }
            } else {
                prefix.append("../");
            }
            return prefix.toString();
        } else {
            log.warn("Expected to find a repoPath on the request but none was found. " +
                    "Perhaps login redirection was required.");
            return null;
        }
    }
}
