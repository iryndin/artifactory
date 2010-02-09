package org.artifactory.webapp.wicket.component;

import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;

/**
 * @author Yoav Aharoni
 */
public class CancelDefaultDecorator extends AjaxCallDecorator {
    @SuppressWarnings({"RefusedBequest"})
    @Override
    public CharSequence decorateScript(CharSequence script) {
        return script + "return cEvent(event);";
    }
}
