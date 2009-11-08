package org.artifactory.webapp.wicket.page.config.proxy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.util.AjaxUtils;

/**
 * @author Tomer Cohen
 */
public class ConfirmModalPanel extends BaseModalPanel {

    private boolean replace = false;

    public ConfirmModalPanel() {
        //setWidth(500);
        Form form = new Form("form");
        setTitle(
                "Do you wish to use this proxy with existing remote repositories (and override any assigned proxies)?");
        add(form);
        TitledBorder border = new TitledBorder("border");
        form.add(border);
        TitledAjaxSubmitLink okButton = new TitledAjaxSubmitLink("okButton", "YES", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                replace = true;
                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }
        };
        border.add(okButton);
        TitledAjaxSubmitLink noButton = new TitledAjaxSubmitLink("noButton", "NO", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // Do nothing
                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }
        };
        border.add(noButton);
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }
}
