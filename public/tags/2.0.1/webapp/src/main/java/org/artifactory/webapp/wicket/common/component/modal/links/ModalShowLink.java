package org.artifactory.webapp.wicket.common.component.modal.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.webapp.wicket.common.component.SimpleLink;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;

/**
 * @author Yoav Aharoni
 */
public abstract class ModalShowLink extends SimpleLink {
    protected ModalShowLink(String id) {
        super(id, id);
    }

    protected ModalShowLink(String id, String title) {
        super(id, title);
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        ModalHandler modalHandler = ModalHandler.getInstanceFor(this);
        modalHandler.setModalPanel(getModelPanel());
        modalHandler.show(target);
    }

    protected abstract BaseModalPanel getModelPanel();
}
