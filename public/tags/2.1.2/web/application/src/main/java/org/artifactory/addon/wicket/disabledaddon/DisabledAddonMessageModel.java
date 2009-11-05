package org.artifactory.addon.wicket.disabledaddon;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.artifactory.addon.AddonInfo;
import org.artifactory.addon.wicket.Addon;

/**
 * @author Yoav Aharoni
 */
public class DisabledAddonMessageModel extends StringResourceModel {
    private static final String MESSAGE_KEY = "addon.disabled";

    public DisabledAddonMessageModel(Addon addon) {
        super(MESSAGE_KEY, null, new Model(addon));
    }

    public DisabledAddonMessageModel(AddonInfo addon) {
        super(MESSAGE_KEY, null, new Model(addon));
    }
}
