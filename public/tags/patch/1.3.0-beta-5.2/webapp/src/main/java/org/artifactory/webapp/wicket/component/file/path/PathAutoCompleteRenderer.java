package org.artifactory.webapp.wicket.component.file.path;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;

import java.io.File;

/**
 * @author yoava
 */
public class PathAutoCompleteRenderer extends AbstractAutoCompleteTextRenderer {
    private PathHelper pathHelper;

    public PathAutoCompleteRenderer(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    @Override
    protected String getTextValue(Object object) {
        File file = (File) object;
        if (file == null) {
            return "";
        }

        return pathHelper.getRelativePath(file);
    }
}