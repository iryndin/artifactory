package org.artifactory.webapp.wicket.application;

import org.apache.wicket.Response;
import org.apache.wicket.authorization.AuthorizationException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ArtifactoryRequestCycle extends WebRequestCycle {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryRequestCycle.class);

    private WebRequest request;

    public ArtifactoryRequestCycle(WebApplication application, WebRequest request,
            Response response) {
        super(application, request, response);
    }

    /**
     * Only login automatically as anonymous if anonymous access is enabled and the browser does not send a previous
     * login cookie, in which case we wish to be thrown to the session expired page.
     */
    @Override
    protected void onBeginRequest() {
        super.onBeginRequest();
        ArtifactoryWebSession session = ArtifactoryWebSession.get();
        AuthorizationService authService = ContextHelper.get().getAuthorizationService();
        if (!session.isSignedIn() && authService.isAnonAccessEnabled()) {
            session.signIn(UserInfo.ANONYMOUS, "");
        }
        session.attach();
    }

    @Override
    protected void onEndRequest() {
        //Remove the repoPath from the request
        WebRequest webRequest = getWebRequest();
        WebUtils.removeRepoPath(webRequest, false);
        super.onEndRequest();
    }

    @Override
    public WebRequest getWebRequest() {
        if (request == null) {
            WebRequest origRequest = super.getWebRequest();
            //If not a repoPath browsing request return the original
            RepoPath repoPath = WebUtils.getRepoPath(origRequest);
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
}
