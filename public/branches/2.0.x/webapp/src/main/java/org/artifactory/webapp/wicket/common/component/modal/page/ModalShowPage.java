package org.artifactory.webapp.wicket.common.component.modal.page;

import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.page.base.BasePage;

/**
 * @author Yoav Aharoni
 */
public class ModalShowPage extends BasePage {
    private BaseModalPanel modalPanel;

    public ModalShowPage(BaseModalPanel modalPanel) {
        this.modalPanel = modalPanel;
        add(modalPanel);
    }

    @Override
    protected String getPageName() {
        return modalPanel.getTitle();
    }

    @Override
    protected Class<? extends BasePage> getMenuPageClass() {
        return ArtifactoryApplication.get().getHomePage();
    }
}
