package org.artifactory.webapp.wicket.application.test;

import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.webapp.wicket.application.ArtifactoryRequestCycle;

/**
 * @author Yoav Aharoni
 */
public class TestRequestCycle extends ArtifactoryRequestCycle {
    private MarkupIdInjector.AjaxCycleData ajaxData;

    public TestRequestCycle(WebApplication application, WebRequest request, Response response) {
        super(application, request, response);
    }

    public void setAjaxData(MarkupIdInjector.AjaxCycleData ajaxData) {
        this.ajaxData = ajaxData;
    }

    public MarkupIdInjector.AjaxCycleData getAjaxData() {
        return ajaxData;
    }

    @Override
    protected void onEndRequest() {
        ajaxData = null;
        super.onEndRequest();
    }
}
