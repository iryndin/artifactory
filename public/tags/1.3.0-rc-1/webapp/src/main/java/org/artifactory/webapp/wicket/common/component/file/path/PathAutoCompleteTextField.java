package org.artifactory.webapp.wicket.common.component.file.path;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.file.File;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.ImprovedAutoCompleteBehavior;

import java.util.Iterator;

public class PathAutoCompleteTextField extends AutoCompleteTextField {
    private static final AutoCompleteSettings SETTINGS = new AutoCompleteSettings().setShowListOnEmptyInput(true).setMaxHeightInPx(200);
    private static final ResourceReference AUTOCOMPLETE_JS = new JavascriptResourceReference(
            ImprovedAutoCompleteBehavior.class, "improved-autocomplete.js");

    private PathMask mask = PathMask.ALL;
    private PathHelper pathHelper;
    private PathAutoCompleteConverter converter;

    public PathAutoCompleteTextField(String id) {
        this(id, null, new PathHelper());
    }

    public PathAutoCompleteTextField(String id, IModel model) {
        this(id, model, new PathHelper());
    }

    public PathAutoCompleteTextField(String id, String root) {
        this(id, null, root);
    }

    public PathAutoCompleteTextField(String id, IModel model, String root) {
        this(id, model, new PathHelper(root));
    }

    protected PathAutoCompleteTextField(String id, IModel model, PathHelper pathHelper) {
        super(id, model, File.class, new PathAutoCompleteRenderer(pathHelper), SETTINGS);

        this.pathHelper = pathHelper;
        converter = new PathAutoCompleteConverter(pathHelper);

        add(new CssClass("text pathAutoComplete"));
        add(new SimpleAttributeModifier("autoCompleteCssClass", "wicket-aa pathAutoComplete_menu"));
    }

    @Override
    protected AutoCompleteBehavior newAutoCompleteBehavior(IAutoCompleteRenderer renderer, AutoCompleteSettings settings) {
        return new AutoCompleteBehavior(renderer, settings) {

            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                response.renderJavascriptReference(AUTOCOMPLETE_JS);
            }

            @Override
            protected Iterator getChoices(String input) {
                return PathAutoCompleteTextField.this.getChoices(input);
            }
        };
    }

    public PathMask getMask() {
        return mask;
    }

    public void setMask(PathMask mask) {
        this.mask = mask;
    }

    public String getRoot() {
        return pathHelper.getRoot();
    }

    public void setRoot(String root) {
        pathHelper.setRoot(root);
    }

    @Override
    protected Iterator getChoices(String input) {
        return pathHelper.getFiles(input, mask).iterator();
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    public IConverter getConverter(Class type) {
        return converter;
    }
}
