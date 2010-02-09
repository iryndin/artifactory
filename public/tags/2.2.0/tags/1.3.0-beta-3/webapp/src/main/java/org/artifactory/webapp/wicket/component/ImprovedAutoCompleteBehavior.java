package org.artifactory.webapp.wicket.component;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.IHeaderResponse;

/**
 * @author yoava
 */

public abstract class ImprovedAutoCompleteBehavior extends AutoCompleteBehavior {

    private static final ResourceReference IMPROVED_AUTOCOMPLETE_JS =
            new ResourceReference(ImprovedAutoCompleteBehavior.class, "improved-autocomplete.js");

    protected ImprovedAutoCompleteBehavior(IAutoCompleteRenderer renderer) {
        super(renderer);
    }

    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderJavascriptReference(IMPROVED_AUTOCOMPLETE_JS);
    }

}
