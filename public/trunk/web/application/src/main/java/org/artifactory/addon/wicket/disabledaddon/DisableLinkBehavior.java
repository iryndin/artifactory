package org.artifactory.addon.wicket.disabledaddon;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.value.IValueMap;

/**
 * @author Yoav Aharoni
 */
public class DisableLinkBehavior extends AbstractBehavior {
    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        IValueMap attributes = tag.getAttributes();
        attributes.remove("href");
        attributes.remove("onclick");
    }
}
