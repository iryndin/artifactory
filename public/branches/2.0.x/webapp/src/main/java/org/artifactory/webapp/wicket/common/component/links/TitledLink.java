package org.artifactory.webapp.wicket.common.component.links;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.ILinkListener;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

/**
 * @author Yoav Aharoni
 */
public abstract class TitledLink extends BaseTitledLink implements ILinkListener {

    protected TitledLink(String id) {
        super(id);
    }

    protected TitledLink(String id, IModel titleModel) {
        super(id, titleModel);
    }

    protected TitledLink(String id, String title) {
        super(id, title);
    }

    public abstract void onClick();

    public void onLinkClicked() {
        onClick();
    }

    protected CharSequence getURL() {
        return urlFor(INTERFACE);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (isEnabled()) {
            CharSequence url = getURL();
            tag.put("href", Strings.replaceAll(url, "&", "&amp;"));
        }
    }
}
