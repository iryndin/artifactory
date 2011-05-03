package org.artifactory.common.wicket.resources.dojo;

import org.artifactory.common.wicket.contributor.ResourcePackage;

/**
 * @author Yoav Aharoni
 */
public class DojoPackage extends ResourcePackage {
    public DojoPackage() {
        // add config
        addJavaScriptTemplate();

        // add dojo js
        if (isDebug()) {
            addJavaScript("excluded/source/dojo/dojo.js.uncompressed.js");
            addJavaScript("excluded/source/dojo/artifactory-dojo.js.uncompressed.js");
        } else {
            addJavaScript("release/dojo/dojo.js");
            addJavaScript("release/dojo/artifactory-dojo.js");
        }

        // add themes
        addCss("release/dojo/resources/dojo.css");
        addCss("release/dijit/themes/tundra/tundra.css");
    }

    public boolean isDebug() {
        return false;
        //        return Application.get().getDebugSettings().isAjaxDebugModeEnabled();
    }
}
