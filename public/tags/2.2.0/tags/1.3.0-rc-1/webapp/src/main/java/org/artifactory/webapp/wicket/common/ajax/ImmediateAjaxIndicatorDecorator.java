package org.artifactory.webapp.wicket.common.ajax;

import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;

/**
 * @author Yoav Aharoni
 */
public class ImmediateAjaxIndicatorDecorator extends AjaxCallDecorator {
    @Override
    public CharSequence decorateScript(CharSequence script) {
        return "ajaxIndicator.disableDelay();" + script;
    }
}