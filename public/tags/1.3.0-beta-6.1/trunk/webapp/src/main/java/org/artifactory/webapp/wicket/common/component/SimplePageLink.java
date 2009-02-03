package org.artifactory.webapp.wicket.common.component;

import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.string.Strings;
import org.artifactory.webapp.wicket.common.component.links.BaseTitledLink;

public class SimplePageLink extends BaseTitledLink {
    private final Class<? extends Page> pageClass;

    public SimplePageLink(String id, String caption, Class<? extends Page> pageClass) {
        super(id, caption);
        this.pageClass = pageClass;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        if (!isLinkEnabled()) {
            disableLink(tag);
        } else {
            CharSequence url = Strings.replaceAll(urlFor(pageClass, null), "&", "&amp;");
            if (tag.getName().equalsIgnoreCase("a")
                    || tag.getName().equalsIgnoreCase("link")
                    || tag.getName().equalsIgnoreCase("area")) {
                tag.put("href", url);
            } else {
                tag.put("onclick", "window.location.href='" + url + "';");
            }
        }
    }
}
