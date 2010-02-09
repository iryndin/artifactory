package org.artifactory.webapp.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.artifactory.security.RepoPath;
import org.artifactory.webapp.wicket.utils.ServletUtils;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ArtifactoryRequestCycle extends WebRequestCycle {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryRequestCycle.class);

    private WebRequest request;

    public ArtifactoryRequestCycle(WebApplication application, WebRequest request,
            Response response) {
        super(application, request, response);
    }

    @Override
    protected void onBeginRequest() {
        super.onBeginRequest();
        ArtifactorySession session = ArtifactorySession.get();
        session.attach();
    }

    @Override
    protected void onEndRequest() {
        //Remove the repoPath from the request
        WebRequest webRequest = getWebRequest();
        ServletUtils.removeRepoPath(webRequest);
        super.onEndRequest();
    }

    @Override
    public WebRequest getWebRequest() {
        if (request == null) {
            WebRequest origRequest = super.getWebRequest();
            //If not a repoPath browsing request return the original
            RepoPath repoPath = ServletUtils.getRepoPath(origRequest);
            if (repoPath != null) {
                request = new RepoPathBrowsingWebRequest(origRequest);
                setRequest(request);
            } else {
                request = origRequest;
            }
        }
        return request;
    }
}
