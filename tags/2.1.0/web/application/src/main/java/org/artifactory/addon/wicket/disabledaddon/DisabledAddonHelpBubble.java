package org.artifactory.addon.wicket.disabledaddon;

import org.artifactory.addon.wicket.Addon;
import org.artifactory.common.wicket.component.help.HelpBubble;

/**
 * @author Yoav Aharoni
 */
public class DisabledAddonHelpBubble extends HelpBubble {
    public DisabledAddonHelpBubble(String id, Addon addon) {
        super(id, new DisabledAddonMessageModel(addon));
    }
}
