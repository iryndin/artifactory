package org.artifactory.webapp.wicket.component.file.path;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.file.File;
import org.artifactory.webapp.wicket.component.ImprovedAutoCompleteBehavior;

import java.util.Iterator;

public class PathAutoCompleteTextField extends AutoCompleteTextField {
    private PathHelper pathHelper;
    private PathMask mask;

    public PathAutoCompleteTextField(String id, String root) {
        this(id, null, root, PathMask.BOTH);
    }

    public PathAutoCompleteTextField(String id, String root, PathMask mask) {
        this(id, null, root, mask);
    }

    public PathAutoCompleteTextField(String id, IModel model, String root) {
        this(id, model, root, PathMask.BOTH);
    }

    public PathAutoCompleteTextField(String id, IModel model, String root, PathMask mask) {
        this(id, model, new PathHelper(root), mask);
    }

    public PathAutoCompleteTextField(String id, IModel model, PathHelper pathHelper,
                                     PathMask mask) {
        super(id, model, File.class, new PathAutoCompleteRenderer(pathHelper), false);

        this.pathHelper = pathHelper;
        this.mask = mask;

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
        this.pathHelper.setRoot(root);
    }

    @Override
    protected Iterator getChoices(String input) {
        return pathHelper.getFiles(input, mask).iterator();
    }

    @Override
    public IConverter getConverter(Class type) {
        return new PathAutoCompleteConverter(pathHelper);
    }
}
