package org.artifactory.webapp.wicket.behavior.collapsable;

import org.artifactory.webapp.wicket.behavior.template.TemplateBehavior;

/**
 * @author Yoav Aharoni
 */
public class CollapsibleBehavior extends TemplateBehavior {
    @Override
    protected boolean hasJavascriptContribution() {
        return true;
    }
}
