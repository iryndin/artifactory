package org.artifactory.common.wicket.resources.domutils;

import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.resources.dojo.DojoPackage;

/**
 * @author Yoav Aharoni
 */
public class CommonJsPackage extends ResourcePackage {
    public CommonJsPackage() {
        dependsOn(new DojoPackage());

        addJavaScript("DomUtils.js");
        addJavaScript("AjaxIndicator.js");
    }
}
