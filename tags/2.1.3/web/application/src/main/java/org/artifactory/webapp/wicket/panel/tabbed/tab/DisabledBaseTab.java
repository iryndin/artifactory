package org.artifactory.webapp.wicket.panel.tabbed.tab;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.addon.wicket.disabledaddon.DisableLinkBehavior;

/**
 * Tab which is disabled by default
 *
 * @author Noam Y. Tenne
 */
public class DisabledBaseTab extends BaseTab {

    public DisabledBaseTab(String title) {
        super(title);
    }

    protected DisabledBaseTab(IModel title) {
        super(title);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void onNewTabLink(Component link) {
        super.onNewTabLink(link);
        link.add(new DisableLinkBehavior());
    }

    @Override
    public Panel getPanel(String panelId) {
        return null;
    }
}
