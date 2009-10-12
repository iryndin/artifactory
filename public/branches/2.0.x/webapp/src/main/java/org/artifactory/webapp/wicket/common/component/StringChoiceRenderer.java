package org.artifactory.webapp.wicket.common.component;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

/**
 * @author Yoav Aharoni
 */
public class StringChoiceRenderer implements IChoiceRenderer {
    private static final StringChoiceRenderer INSTANCE = new StringChoiceRenderer();

    public Object getDisplayValue(Object object) {
        return object.toString();
    }

    public String getIdValue(Object object, int index) {
        return String.valueOf(index);
    }

    public static StringChoiceRenderer getInstance() {
        return INSTANCE;
    }
}
