package org.artifactory.webapp.wicket.common.component.modal.page;

import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.page.base.BasePage;

/**
 * @author Yoav Aharoni
 */
public class ModalShowPage extends BasePage {
    private BaseModalPanel modalPanel;

    public ModalShowPage(BaseModalPanel modalPanel) {
        add(modalPanel);
    }

    @Override
    protected String getPageName() {
        return modalPanel.getTitle();
    }
}
