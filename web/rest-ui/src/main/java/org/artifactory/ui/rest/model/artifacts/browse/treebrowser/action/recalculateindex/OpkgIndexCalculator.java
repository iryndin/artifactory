package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex;

import org.artifactory.addon.opkg.OpkgAddon;
import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Dan Feldman
 */
@JsonTypeName("Opkg")
public class OpkgIndexCalculator extends BaseIndexCalculator {

    @Override
    public void calculateIndex() throws Exception {
        OpkgAddon opkgAddon = addonsManager.addonByType(OpkgAddon.class);
        if(opkgAddon != null) {
            opkgAddon.recalculateEntireRepo(getRepoKey(), null, true, false);
        }
    }
}
