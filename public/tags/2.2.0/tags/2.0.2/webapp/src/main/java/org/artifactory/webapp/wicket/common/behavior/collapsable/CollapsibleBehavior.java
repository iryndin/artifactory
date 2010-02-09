package org.artifactory.webapp.wicket.common.behavior.collapsable;

import org.artifactory.webapp.wicket.common.behavior.template.TemplateBehavior;

/**
 * @author Yoav Aharoni
 */
public class CollapsibleBehavior extends TemplateBehavior {
    @Override
    protected boolean hasJavascriptContribution() {
        return true;
    }
}
