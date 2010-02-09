package org.artifactory.webapp.wicket.common.component.modal;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.webapp.wicket.common.TitleModel;
import org.artifactory.webapp.wicket.common.component.modal.page.ModalShowPage;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;

/**
 * @author Yoav Aharoni
 */
public class ModalHandler extends ModalWindow implements TitleModel {
    public static final String CONTENT_ID = "content";
    private static final WebMarkupContainer EMPTY_CONTENT = new WebMarkupContainer(CONTENT_ID);

    /**
     * <b>DO NOT USE THIS CONSTRUCTOR!</b><br/>
     * Use <b>ModalHandler.getInstanceFor(this)</b> instead.
     *
     * @param id id
     */
    public ModalHandler(String id) {
        super(id);
        setCssClassName("w_modal");

        add(HeaderContributor.forJavaScript(ModalHandler.class, "ModalHandler.js"));
        setCloseButtonCallback(new CloseButtonCallback() {
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                ModalHandler.this.onCloseButtonClicked(target);
                return true;
            }
        });

        setWindowClosedCallback(new WindowClosedCallback() {
            public void onClose(AjaxRequestTarget target) {
                ModalHandler.this.onClose(target);
            }
        });
    }

    public void setModalPanel(BaseModalPanel modalPanel) {
        setContent(modalPanel);
    }

    @Override
    public void setContent(Component component) {
        String cookieName = getId() + "-" + component.getClass().getSimpleName();
        setCookieName(cookieName);

        // set content first
        super.setContent(component);

        if (component instanceof BaseModalPanel) {
            BaseModalPanel modalPanel = (BaseModalPanel) component;

            setTitle(modalPanel.getTitle());

            setMinimalWidth(modalPanel.getMinimalWidth());
            setMinimalHeight(modalPanel.getMinimalHeight());

            setInitialWidth(modalPanel.getInitialWidth());
            setInitialHeight(modalPanel.getInitialHeight());
        }
    }

    protected void onCloseButtonClicked(AjaxRequestTarget target) {
        BaseModalPanel modalPanel = getModalPanel();
        if (modalPanel != null) {
            modalPanel.onCloseButtonClicked(target);
        }
    }

    protected void onClose(AjaxRequestTarget target) {
        BaseModalPanel modalPanel = getModalPanel();
        if (modalPanel != null) {
            modalPanel.onClose(target);
        }
        target.appendJavascript("ModalHandler.onClose();");
        setContent(EMPTY_CONTENT);
    }

    @Override
    public void show(AjaxRequestTarget target) {
        super.show(target);
        // move modal panel into mainForm, so it would be submited
        target.appendJavascript("ModalHandler.onPopup();");

        // call event listener
        BaseModalPanel modalPanel = getModalPanel();
        if (modalPanel != null) {
            modalPanel.onShow(target);
        }
    }

    private BaseModalPanel getModalPanel() {
        Component content = get(BaseModalPanel.MODAL_ID);
        if (content instanceof BaseModalPanel) {
            return (BaseModalPanel) content;
        }

        return null;
    }

    public static ModalHandler getInstanceFor(Component component) {
        HasModalHandler container;
        if (component instanceof HasModalHandler) {
            container = (HasModalHandler) component;
        } else {
            container = (HasModalHandler) component.findParent(HasModalHandler.class);
        }

        return container.getModalHandler();
    }

    public void showPage() {
        BaseModalPanel modalPanel = getModalPanel();
        if (modalPanel != null) {
            setResponsePage(new ModalShowPage(modalPanel));
        }
    }
}
