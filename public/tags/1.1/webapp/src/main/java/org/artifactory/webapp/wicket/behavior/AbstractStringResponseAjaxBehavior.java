package org.artifactory.webapp.wicket.behavior;

import org.apache.log4j.Logger;
import wicket.IRequestTarget;
import wicket.Page;
import wicket.RequestCycle;
import wicket.behavior.AbstractAjaxBehavior;
import wicket.request.target.basic.StringRequestTarget;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class AbstractStringResponseAjaxBehavior extends AbstractAjaxBehavior {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AbstractStringResponseAjaxBehavior.class);

    private static final long serialVersionUID = 1L;

    protected void onBind() {
        getComponent().setOutputMarkupId(true);
    }

    public final void onRequest() {
        boolean isPageVersioned = true;
        Page page = getComponent().getPage();
        try {
            isPageVersioned = page.isVersioned();
            page.setVersioned(false);
            String response = getResponse();
            RequestCycle cycle = RequestCycle.get();
            boolean redirect = cycle.getRedirect();
            IRequestTarget target = new StringRequestTarget(response);
            cycle.setRequestTarget(target);
        } finally {
            page.setVersioned(isPageVersioned);
        }
    }

    /**
     * Return a string response or a redirect-to url
     *
     * @return
     */
    protected abstract String getResponse();
}
