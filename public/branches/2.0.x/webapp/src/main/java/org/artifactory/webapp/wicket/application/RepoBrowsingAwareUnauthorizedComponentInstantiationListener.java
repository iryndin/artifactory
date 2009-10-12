package org.artifactory.webapp.wicket.application;

import org.apache.wicket.Component;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.webapp.wicket.utils.WebUtils;

/**
 * @author Yoav Aharoni
 */
public class RepoBrowsingAwareUnauthorizedComponentInstantiationListener
        implements IUnauthorizedComponentInstantiationListener {
    private IUnauthorizedComponentInstantiationListener delegate;

    public RepoBrowsingAwareUnauthorizedComponentInstantiationListener(
            IUnauthorizedComponentInstantiationListener delegate) {
        this.delegate = delegate;
    }

    public void onUnauthorizedInstantiation(Component component) {
        WebRequest webRequest = (WebRequest) component.getRequest();
        WebUtils.removeRepoPath(webRequest, true);
        delegate.onUnauthorizedInstantiation(component);
    }

}
