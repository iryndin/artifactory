package org.artifactory.addon.wicket.disabledaddon;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.value.IValueMap;
import org.artifactory.addon.wicket.Addon;

/**
 * @author Yoav Aharoni
 */
public class DisabledAddonBehavior extends AddonNeededBehavior {
    public DisabledAddonBehavior(Addon addon) {
        super(addon);
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        IValueMap attributes = tag.getAttributes();
        attributes.remove("href");
        attributes.remove("onclick");
    }
}
