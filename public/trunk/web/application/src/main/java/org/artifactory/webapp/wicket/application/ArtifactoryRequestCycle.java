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

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.authorization.AuthorizationException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.request.target.resource.SharedResourceRequestTarget;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.servlet.RequestUtils;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ArtifactoryRequestCycle extends WebRequestCycle {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryRequestCycle.class);

    private WebRequest request;

    public ArtifactoryRequestCycle(WebApplication application, WebRequest request, Response response) {
        super(application, request, response);
    }

    @Override
    protected void onRequestTargetSet(IRequestTarget requestTarget) {
        super.onRequestTargetSet(requestTarget);
        if (requestTarget instanceof SharedResourceRequestTarget) {
            setAutomaticallyClearFeedbackMessages(false);
        }
    }

    /**
     * Only login automatically as anonymous if anonymous access is enabled and the browser does not send a previous
     * login cookie, in which case we wish to be thrown to the session expired page.
     */
    @Override
    protected void onBeginRequest() {
        super.onBeginRequest();
        //Only handle authentication for non-shared-resources requests
        if (isSharedResourcesRequest()) {
            return;
        }
        ArtifactoryWebSession session = ArtifactoryWebSession.get();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (!session.isSignedIn() ||
                    (session.isAnonymous() && !UserInfo.ANONYMOUS.equals("" + authentication.getPrincipal()))) {
                // session is not logged in yet, but was already authenticated (probably by a filter) so set the
                // authentication and call signIn with empty params which will mark the session as authenticated
                session.setAuthentication(authentication);
                session.signIn("" + authentication.getPrincipal(), "");
            }
        }
        session.bindAuthentication();
    }

    @Override
    protected void onEndRequest() {
        //Remove the repoPath from the request
        WebRequest webRequest = getWebRequest();
        RequestUtils.removeRepoPath(webRequest, false);
        super.onEndRequest();
    }

    @Override
    public WebRequest getWebRequest() {
        if (request == null) {
            WebRequest origRequest = super.getWebRequest();
            //If not a repoPath browsing request return the original
            RepoPath repoPath = RequestUtils.getRepoPath(origRequest);
            if (repoPath != null) {
                request = new RepoPathBrowsingWebRequest(origRequest);
                setRequest(request);
            } else {
                request = origRequest;
            }
        }
        return request;
    }

    @Override
    protected void logRuntimeException(RuntimeException e) {
        // AuthorizationException should be loged without stack trace
        if (e instanceof AuthorizationException) {
            StringBuilder builder = new StringBuilder("User ");
            try {
                AuthorizationService authService = ContextHelper.get().getAuthorizationService();
                builder.append(authService.currentUsername());
            } catch (Exception e1) {
                log.error("Error retrieving Username", e1);
            }
            builder.append(" accessed unauthorized resource. ");
            builder.append(e.getMessage());
            log.error(builder.toString());
        } else {
            super.logRuntimeException(e);
        }
    }

    private boolean isSharedResourcesRequest() {
        //Use current request, as the private request field can be null
        Request currentRequest = getRequest();
        String path = currentRequest.getRequestParameters().getPath();
        String sharedResourcesPath = ((ArtifactoryApplication) getApplication()).getSharedResourcesPath();
        return path.startsWith(sharedResourcesPath);
    }
}
