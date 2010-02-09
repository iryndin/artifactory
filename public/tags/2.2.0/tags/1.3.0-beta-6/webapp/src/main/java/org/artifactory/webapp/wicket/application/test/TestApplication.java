package org.artifactory.webapp.wicket.application.test;

import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;

/**
 * @author Yoav Aharoni
 */
public class TestApplication extends ArtifactoryApplication {
    private MarkupIdInjector testMakupIdInjector;

    @Override
    protected void init() {
        super.init();

        //DISABLE THE MARKUP ID COSTOMIZATION TEMPORARILY!!!!!
        //Add the makup id injector
        testMakupIdInjector = new MarkupIdInjector();
        addComponentOnBeforeRenderListener(testMakupIdInjector);
    }

    @Override
    public AjaxRequestTarget newAjaxRequestTarget(Page page) {
        testMakupIdInjector.setInAjaxRequest();
        return super.newAjaxRequestTarget(page);
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    public RequestCycle newRequestCycle(Request request, Response response) {
        return new TestRequestCycle(this, (WebRequest) request, response);
    }
}
