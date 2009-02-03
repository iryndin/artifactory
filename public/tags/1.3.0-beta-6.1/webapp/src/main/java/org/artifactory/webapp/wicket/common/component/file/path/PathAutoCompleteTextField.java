package org.artifactory.webapp.wicket.common.component.file.path;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.file.File;
import org.artifactory.webapp.wicket.common.component.ImprovedAutoCompleteBehavior;

import java.util.Iterator;

public class PathAutoCompleteTextField extends AutoCompleteTextField {
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
        super(id, model, File.class, new PathAutoCompleteRenderer(pathHelper), false);

        this.pathHelper = pathHelper;
        converter = new PathAutoCompleteConverter(pathHelper);

        add(new AttributeAppender("class", true, new Model("pathAutoComplete"), " "));
        add(new SimpleAttributeModifier("autoCompleteCssClass", "wicket-aa pathAutoComplete_menu"));
        add(HeaderContributor.forJavaScript(ImprovedAutoCompleteBehavior.class, "improved-autocomplete.js"));
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
