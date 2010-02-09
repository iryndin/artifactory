package org.artifactory.webapp.wicket.common.component.modal.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.webapp.wicket.common.component.SimpleLink;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;

/**
 * @author Yoav Aharoni
 */
public class ModalCloseLink extends SimpleLink {
    public ModalCloseLink(String id) {
        super(id, "Cancel");
    }

    public ModalCloseLink(String id, String title) {
        super(id, title);
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        ModalHandler.closeCurrent(target);
    }
}
