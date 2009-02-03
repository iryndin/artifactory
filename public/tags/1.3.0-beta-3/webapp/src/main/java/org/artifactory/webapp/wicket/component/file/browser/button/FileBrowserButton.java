package org.artifactory.webapp.wicket.component.file.browser.button;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.artifactory.webapp.wicket.component.file.browser.panel.FileBrowserPanel;
import org.artifactory.webapp.wicket.component.file.path.PathHelper;
import org.artifactory.webapp.wicket.component.file.path.PathMask;

/**
 * @author Yoav Aharoni
 */
public class FileBrowserButton extends Panel {
    private PathHelper pathHelper;
    private ModalWindow modalWindow;
    private PathMask mask;

    public FileBrowserButton(String id, String root) {
        this(id, null, root, PathMask.BOTH);
    }

    public FileBrowserButton(String id, String root, PathMask mask) {
        this(id, null, root, mask);
    }

    public FileBrowserButton(String id, IModel model, String root) {
        this(id, model, root, PathMask.BOTH);
    }

    public FileBrowserButton(String id, IModel model, String root, PathMask mask) {
        this(id, model, new PathHelper(root), mask);
    }

    public FileBrowserButton(String id, IModel model, PathHelper pathHelper, PathMask mask) {
        super(id);

        if (model != null) {
            setModel(model);
        }

        this.pathHelper = pathHelper;
        this.mask = mask;

        init();
    }

    protected void init() {
        add(HeaderContributor.forCss(FileBrowserPanel.class, "style/filebrowser.css"));
        modalWindow = new ModalWindow("modalWindow");
        modalWindow.setInitialHeight(280);
        modalWindow.setInitialWidth(580);
        modalWindow.setResizable(false);
        modalWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                onCancelClicked(target);
                return true;
            }
        });
        add(modalWindow);

        add(new BrowseLink("browseLink"));
    }

    protected void onOkClicked(AjaxRequestTarget target) {
    }

    protected void onCancelClicked(AjaxRequestTarget target) {
    }

    protected void onShowBrowserClicked(AjaxRequestTarget target) {
        FileBrowserPanel fileBrowserPanel = new MyFileBrowserPanel(modalWindow.getContentId());

        modalWindow.setContent(fileBrowserPanel);
        modalWindow.setTitle(fileBrowserPanel.getTitle());
        modalWindow.show(target);
    }

    private void closeFileBrowser(AjaxRequestTarget target) {
        modalWindow.close(target);
        modalWindow.setContent(new WebMarkupContainer(modalWindow.getContentId()));
    }


    private class BrowseLink extends WebComponent {
        public BrowseLink(String id) {
            super(id);
            add(new AjaxEventBehavior("onclick") {
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    onShowBrowserClicked(target);
                }
            });

            add(new AttributeModifier("title", new StringResourceModel("browse", this, null)));
        }
    }

    private class DelegetedModelModel implements IModel {
        public Object getObject() {
            return FileBrowserButton.this.getModelObject();
        }

        public void setObject(Object object) {
            FileBrowserButton.this.setModelObject(object);
        }

        public void detach() {
        }
    }

    private class MyFileBrowserPanel extends FileBrowserPanel {
        public MyFileBrowserPanel(String id) {
            super(id, new DelegetedModelModel(), pathHelper, mask);
        }

        @Override
        protected void onCancelClicked(AjaxRequestTarget target) {
            FileBrowserButton.this.onCancelClicked(target);
            closeFileBrowser(target);
        }

        @Override
        protected void onOkClicked(AjaxRequestTarget target) {
            FileBrowserButton.this.onOkClicked(target);
            closeFileBrowser(target);
        }
    }
}
