package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.modal.panel.bordered.BorderedModalPanel;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ViewAction;
import org.artifactory.webapp.actionable.event.ItemEvent;

/**
 * Displays a build XML in a modal window
 *
 * @author Noam Y. Tenne
 */
public class ViewBuildXmlAction extends ItemAction {

    public static final String ACTION_NAME = "View Build XML";
    private ModalHandler textContentViewer;
    private String buildXml;

    /**
     * Main constructor
     *
     * @param textContentViewer Modal handler for displaying the build XML
     * @param buildXml          Build XML to display
     */
    public ViewBuildXmlAction(ModalHandler textContentViewer, String buildXml) {
        super(ACTION_NAME);
        this.textContentViewer = textContentViewer;
        this.buildXml = buildXml;
    }

    @Override
    public void onAction(ItemEvent e) {
        TextContentPanel contentPanel = new TextContentPanel(textContentViewer.getContentId());
        contentPanel.setContent(buildXml);
        BaseModalPanel modelPanel = new BorderedModalPanel(contentPanel);
        modelPanel.setTitle("Build Info XML");
        contentPanel.add(new CssClass("modal-code"));
        textContentViewer.setContent(modelPanel);
        AjaxRequestTarget target = e.getTarget();
        textContentViewer.show(target);
    }

    @Override
    public String getCssClass() {
        return ViewAction.class.getSimpleName();
    }
}
