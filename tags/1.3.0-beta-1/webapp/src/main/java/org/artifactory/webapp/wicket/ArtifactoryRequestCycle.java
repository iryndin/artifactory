package org.artifactory.webapp.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.utils.WebUtils;

import javax.servlet.http.Cookie;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ArtifactoryRequestCycle extends WebRequestCycle {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryRequestCycle.class);

    private WebRequest request;
    private ArtifactorySecurityManager securityHelper;

    public ArtifactoryRequestCycle(WebApplication application, WebRequest request,
                                   Response response) {
        super(application, request, response);
        securityHelper = (ArtifactorySecurityManager) LazyInitProxyFactory.createProxy(
                ArtifactorySecurityManager.class, new SecurityHelperLocator());
    }

    /**
     * Only login automatically as anonymous if anonymous access is enabled and the browser does not
     * send a previous login cookie, in which case we wish to be thrown to the session expired
     * page.
     */
    @Override
    protected void onBeginRequest() {
        super.onBeginRequest();
        ArtifactorySession session = ArtifactorySession.get();
        WebRequest request = getWebRequest();
        Cookie cookie = request.getCookie(ArtifactorySession.COOKIE_LAST_USER_ID);
        if (!session.isSignedIn() && securityHelper.isAnonAccessEnabled() && cookie == null) {
            session.signIn(ArtifactorySecurityManager.USER_ANONYMOUS, "");
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

    private static class SecurityHelperLocator implements IProxyTargetLocator {
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        public Object locateProxyTarget() {
            ArtifactoryContext context = ContextHelper.get();
            ArtifactorySecurityManager securityHelper = context.getSecurity();
            return securityHelper;
        }
    }
}
