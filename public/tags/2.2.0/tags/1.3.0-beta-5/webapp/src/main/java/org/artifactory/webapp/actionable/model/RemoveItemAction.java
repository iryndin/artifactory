package org.artifactory.webapp.actionable.model;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.webapp.actionable.ItemAction;
import org.artifactory.webapp.actionable.ItemActionEvent;
import org.artifactory.webapp.wicket.browse.BrowseRepoPanel;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public abstract class RemoveItemAction extends ItemAction {
    protected RemoveItemAction() {
        super(ActionDescriptor.REMOVE.getName());
    }

    @Override
    public void actionPerformed(ItemActionEvent e) {
        remove();
        removeNodeDetailsPanel(e);
    }

    private void removeNodeDetailsPanel(ItemActionEvent e) {
        List<Component> targetComponents = e.getTargetComponents();

        // create dummy panel
        Panel dummy = new EmptyPanel("nodePanel");
        dummy.setOutputMarkupId(true);

        // remove panel
        WebMarkupContainer nodaPanelContainer = (WebMarkupContainer) targetComponents.get(1);
        BrowseRepoPanel browseRepoPanel = (BrowseRepoPanel) nodaPanelContainer.getParent();
        browseRepoPanel.setNodeDisplayPanel(dummy);
        nodaPanelContainer.replace(dummy);
    }

    protected abstract void remove();
}
