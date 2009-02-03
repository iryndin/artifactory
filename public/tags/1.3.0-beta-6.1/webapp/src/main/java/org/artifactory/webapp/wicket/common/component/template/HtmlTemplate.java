package org.artifactory.webapp.wicket.common.component.template;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class HtmlTemplate extends WebComponent {
    private Map<String, IModel> parametersMap;

    public HtmlTemplate(String id) {
        super(id);

        parametersMap = new HashMap<String, IModel>();
        setEscapeModelStrings(false);
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        String rawHtml = readBodyMarkup(markupStream, openTag);
        String interpolatedHtml = ModelVariableInterpolator.interpolate(rawHtml, parametersMap);
        getResponse().write(interpolatedHtml);
    }

    public Map<String, IModel> getParametersMap() {
        return parametersMap;
    }

    public void setParameter(String key, String value) {
        parametersMap.put(key, new Model(value));
    }

    public void setParameter(String key, IModel modelValue) {
        parametersMap.put(key, modelValue);
    }

    public static String readBodyMarkup(MarkupStream markupStream, ComponentTag openTag) {
        StringBuilder innerMarkup = new StringBuilder();
        while (markupStream.hasMore()) {
            if (markupStream.get().closes(openTag)) {
                return innerMarkup.toString();
            }
            innerMarkup.append(markupStream.get().toCharSequence().toString());
            markupStream.next();
        }
        throw new MarkupException(markupStream, "Expected close tag for " + openTag);
    }
}