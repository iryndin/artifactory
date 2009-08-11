/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.application;

import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        int end = path.indexOf("/", begin);
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
        RepoPath repoPath = WebUtils.getRepoPath(this);
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
        RepoPath repoPath = WebUtils.getRepoPath(this);
        if (repoPath != null) {
            String path = repoPath.getPath();
            String prefix;
            if (StringUtils.hasLength(path)) {
                prefix = "";
                int nesting = new StringTokenizer(path, "/").countTokens() + 1;
                while (nesting > 0) {
                    prefix += "../";
                    nesting--;
                }
            } else {
                prefix = "../";
            }
            return prefix;
        } else {
            log.warn("Expected to find a repoPath on the request but none was found. " +
                    "Perhaps login redirection was required.");
            return null;
        }
    }
}
