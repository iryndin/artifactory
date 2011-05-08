/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.common.wicket.component.modal;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.common.wicket.component.modal.page.ModalShowPage;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.model.TitleModel;

/**
 * @author Yoav Aharoni
 */
public class ModalHandler extends ModalWindow implements TitleModel {
    public static final String CONTENT_ID = "content";
    private BaseModalPanel modalPanel;

    private final WebMarkupContainer EMPTY_CONTENT = new WebMarkupContainer(CONTENT_ID);

    /**
     * <b>DO NOT USE THIS CONSTRUCTOR!</b><br/> Use <b>ModalHandler.getInstanceFor(this)</b> instead.
     *
     * @param id id
     */
    public ModalHandler(String id) {
        super(id);
        setCssClassName("w_modal");

        add(ResourcePackage.forJavaScript(ModalHandler.class));
        setCloseButtonCallback(new CloseButtonCallback() {
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                ModalHandler.this.onCloseButtonClicked(target);
                return true;
            }
        });

        setWindowClosedCallback(new WindowClosedCallback() {
            public void onClose(AjaxRequestTarget target) {
                ModalHandler.this.onClose(target);
                modalPanel = null;
            }
        });
    }

    public void setModalPanel(BaseModalPanel modalPanel) {
        setContent(modalPanel);
    }

    @Override
    public ModalWindow setContent(Component component) {
        // set content first
        super.setContent(component);

        // get BaseModalPanel settings
        if (component instanceof BaseModalPanel) {
            BaseModalPanel modalPanel = (BaseModalPanel) component;
            this.modalPanel = modalPanel;

            setTitle(modalPanel.getTitle());
            setCookieName(modalPanel.getCookieName());

            setMinimalWidth(modalPanel.getMinimalWidth());
            setMinimalHeight(modalPanel.getMinimalHeight());
            setInitialWidth(modalPanel.getInitialWidth());
            setInitialHeight(modalPanel.getInitialHeight());
            setResizable(modalPanel.isResizable());
        }
        return this;
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
        return modalPanel;
    }

    public static ModalHandler getInstanceFor(Component component) {
        HasModalHandler container;
        if (component instanceof HasModalHandler) {
            container = (HasModalHandler) component;
        } else {
            container = component.findParent(HasModalHandler.class);
        }

        return container.getModalHandler();
    }

    public void showAsPage(AjaxRequestTarget target) {
        final BaseModalPanel panel = getModalPanel();

        // setPageCreator() will reset getModalPanel()
        setPageCreator(new PageCreator() {
            public Page createPage() {
                return new ModalShowPage(panel);
            }
        });
        show(target);
    }

    public static void resizeAndCenterCurrent(AjaxRequestTarget target) {
        resizeCurrent(target);
        centerCurrent(target);
    }

    public static void resizeCurrent(AjaxRequestTarget target) {
        target.appendJavascript("ModalHandler.resizeCurrent();");
    }

    public static void centerCurrent(AjaxRequestTarget target) {
        target.appendJavascript("ModalHandler.centerCurrent();");
    }
}
