package org.artifactory.webapp.wicket.component.file.path;

import org.apache.wicket.util.convert.IConverter;

import java.io.File;
import java.util.Locale;

public class PathAutoCompleteConverter implements IConverter {
    private PathHelper pathHelper;

    public PathAutoCompleteConverter(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    public Object convertToObject(String value, Locale locale) {
        return pathHelper.getAbsuloteFile(value);
    }

    public String convertToString(Object value, Locale locale) {
        if (value == null) {
            return null;
        }

        File file;
        if (value instanceof String) {
            file = new File((String) value);
        } else {
            file = (File) value;
        }

        return pathHelper.getRelativePath(file);
    }
}
