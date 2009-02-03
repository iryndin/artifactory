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

import java.io.File;

/**
 * @author Yoav Aharoni
 */
public class FileBrowserButton extends Panel {
    private String chRoot;
    private PathHelper pathHelper;
    private ModalWindow modalWindow;
    private PathMask mask = PathMask.ALL;

    public FileBrowserButton(String id) {
        this(id, null, new PathHelper());
    }

    public FileBrowserButton(String id, IModel model) {
        this(id, model, new PathHelper());
    }

    public FileBrowserButton(String id, String root) {
        this(id, null, root);
    }

    public FileBrowserButton(String id, IModel model, String root) {
        this(id, model, new PathHelper(root));
    }

    protected FileBrowserButton(String id, IModel model, PathHelper pathHelper) {
        super(id);

        if (model != null) {
            setModel(model);
        }

        this.pathHelper = pathHelper;
        init();
    }

    protected void init() {
        chRoot = new File(chRoot == null ? "/" : chRoot).getAbsolutePath();
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

    @SuppressWarnings({"UnusedDeclaration"})
    protected void onCancelClicked(AjaxRequestTarget target) {
    }

    protected void onShowBrowserClicked(AjaxRequestTarget target) {
        FileBrowserPanel fileBrowserPanel = new MyFileBrowserPanel(modalWindow.getContentId());
        fileBrowserPanel.setMask(mask);

        modalWindow.setContent(fileBrowserPanel);
        modalWindow.setTitle(fileBrowserPanel.getTitle());
        modalWindow.show(target);
    }

    public PathMask getMask() {
        return mask;
    }

    public void setMask(PathMask mask) {
        this.mask = mask;
    }


    private class BrowseLink extends WebComponent {
        private BrowseLink(String id) {
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
            return getModelObject();
        }

        public void setObject(Object object) {
            setModelObject(object);
        }

        public void detach() {
        }
    }

    private class MyFileBrowserPanel extends FileBrowserPanel {
        private MyFileBrowserPanel(String id) {
            super(id, new DelegetedModelModel(), pathHelper);
            setChRoot(chRoot);
        }

        @Override
        protected void onCancelClicked(AjaxRequestTarget target) {
            super.onCancelClicked(target);
            FileBrowserButton.this.onCancelClicked(target);
            closeFileBrowser(target);
        }

        @Override
        protected void onOkClicked(AjaxRequestTarget target) {
            super.onOkClicked(target);
            FileBrowserButton.this.onOkClicked(target);
            closeFileBrowser(target);
        }

        private void closeFileBrowser(AjaxRequestTarget target) {
            modalWindow.close(target);
            modalWindow.setContent(new WebMarkupContainer(modalWindow.getContentId()));
        }
    }
}
