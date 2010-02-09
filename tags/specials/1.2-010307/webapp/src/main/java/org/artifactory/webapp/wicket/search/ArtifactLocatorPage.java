package org.artifactory.webapp.wicket.search;

import org.artifactory.resource.ArtifactResource;
import org.artifactory.search.SearchControls;
import org.artifactory.search.SearchHelper;
import org.artifactory.utils.DateUtils;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.ContentDialogPanel;
import org.json.JSONArray;
import org.json.JSONObject;
import wicket.RequestCycle;
import wicket.ajax.AbstractDefaultAjaxBehavior;
import wicket.ajax.AjaxRequestTarget;
import wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import wicket.markup.ComponentTag;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.form.TextField;
import wicket.model.PropertyModel;
import wicket.util.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArtifactLocatorPage extends ArtifactoryPage {

    private SearchControls searchControls;
    private List<ArtifactResource> artifacts;

    /**
     * Constructor.
     */
    public ArtifactLocatorPage() {
        searchControls = new SearchControls();
        artifacts = new ArrayList<ArtifactResource>();

        final ContentDialogPanel pomDialogPanel = new ContentDialogPanel("pomDialog");
        add(pomDialogPanel);

        //Add the table
        final WebMarkupContainer resultsPanel = new WebMarkupContainer("results");
        resultsPanel.setOutputMarkupId(true);
        resultsPanel.add(new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                final String selectCall = "{wicketAjaxPost('" + getCallbackUrl()
                        +
                        "&selection='+dojo.widget.manager.getWidgetById('results').getSelectedData().id); return true;}";
                tag.put("onSelect", selectCall);
            }

            protected void respond(AjaxRequestTarget target) {
                RequestCycle rc = RequestCycle.get();
                String selected = rc.getRequest().getParameter("selection");
                int selectedIdx = Integer.parseInt(selected);
                ArtifactResource artifact = artifacts.get(selectedIdx);
                String content = getArtifactMetadataContent(artifact);
                pomDialogPanel.ajaxUpdate(content, target);
            }
        });
        add(resultsPanel);
        final TextField searchControl =
                new TextField("search", new PropertyModel(this, "searchControls.search"));
        searchControl.setOutputMarkupId(true);
        searchControl.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
            private static final long serialVersionUID = 1L;

            protected void onUpdate(AjaxRequestTarget target) {
                artifacts.clear();
                Set<ArtifactResource> results =
                        SearchHelper.searchArtifacts(searchControls, getContext());
                artifacts.addAll(results);
                String jsonResults;
                jsonResults = getJsonResults();
                if (artifacts.isEmpty()) {
                    target.appendJavascript(
                            "dojo.lfx.html.fadeOut('results', 500).play();");
                } else {
                    target.appendJavascript(
                            "dojo.widget.manager.getWidgetById('results').store.setData(" +
                                    jsonResults + ", true);");
                    target.appendJavascript(
                            "dojo.html.setOpacity('results', 0);");
                    target.appendJavascript(
                            "dojo.lfx.html.fadeIn('results', 500).play();");
                }
            }
        }.setThrottleDelay(Duration.milliseconds(1000)));
        add(searchControl);
    }

    protected String getPageName() {
        return "Artifact Locator";
    }

    private String getJsonResults() {
        //return JSONRPCBridge.getGlobalBridge().getSerializer().toJSON(artifacts.toArray(new ArtifactModel[] {}));
        //[{"artifact":null,"javaClass":"org.apache.maven.proxy.webapp.wicket.ArtifactModel","group":null,"relativePath":"\/tmatesoft\/javasvn\/1.0.2","lastModified":"Sun Apr 23 09:28:43 GMT 2006","name":"javasvn-1.0.2-javahl.jar","version":null},{"artifact":null,"javaClass":"org.apache.maven.proxy.webapp.wicket.ArtifactModel","group":null,"relativePath":"\/tmatesoft\/javasvn\/1.0.2","lastModified":"Sun Apr 23 09:28:43 GMT 2006","name":"javasvn-1.0.2.jar","version":null}]
        JSONArray results = new JSONArray();
        int i = 0;
        for (ArtifactResource artifact : artifacts) {
            JSONObject result = new JSONObject();
            result.put("id", i++);
            result.put("name", artifact.getName());
            result.put("relativePath", artifact.getRelPath());
            String lastModified = DateUtils.format(artifact.getLastModified(), getCc());
            result.put("lastModified", lastModified);
            result.put("repositoryName", artifact.getRepoKey());
            results.put(result);
        }
        return results.toString();
    }

    public SearchControls getSearchControls() {
        return searchControls;
    }

    public List<ArtifactResource> getArtifacts() {
        return artifacts;
    }
}
