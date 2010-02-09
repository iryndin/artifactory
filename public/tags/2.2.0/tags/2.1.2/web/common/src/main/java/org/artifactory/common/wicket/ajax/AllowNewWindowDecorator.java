package org.artifactory.common.wicket.ajax;

import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;

/**
 * @author Yoav Aharoni
 */
public class AllowNewWindowDecorator extends AjaxPreprocessingCallDecorator {
    public AllowNewWindowDecorator(IAjaxCallDecorator delegate) {
        super(delegate);
    }

    public AllowNewWindowDecorator() {
        super(null);
    }

    @Override
    public CharSequence preDecorateScript(CharSequence script) {
        return "if (event.metaKey || event.ctrlKey) return true;" + super.preDecorateScript(script);
    }
}
