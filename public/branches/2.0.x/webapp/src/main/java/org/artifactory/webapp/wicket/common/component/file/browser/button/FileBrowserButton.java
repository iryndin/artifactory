package org.artifactory.webapp.wicket.common.component.file.browser.button;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.artifactory.webapp.wicket.common.component.file.browser.panel.FileBrowserPanel;
import org.artifactory.webapp.wicket.common.component.file.path.PathHelper;
import org.artifactory.webapp.wicket.common.component.file.path.PathMask;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;

import java.io.File;

/**
 * @author Yoav Aharoni
 */
public class FileBrowserButton extends Panel {
    private String chRoot;
    private PathHelper pathHelper;
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
        add(new BrowseLink("browseLink"));
    }

    protected void onOkClicked(AjaxRequestTarget target) {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void onCancelClicked(AjaxRequestTarget target) {
    }

    protected void onShowBrowserClicked(AjaxRequestTarget target) {
        FileBrowserPanel fileBrowserPanel = new MyFileBrowserPanel();
        fileBrowserPanel.setMask(mask);

        ModalHandler modalHandler = ModalHandler.getInstanceFor(this);
        modalHandler.setModalPanel(fileBrowserPanel);
        modalHandler.show(target);
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
        private MyFileBrowserPanel() {
            super(new DelegetedModelModel(), pathHelper);
            setChRoot(chRoot);
        }

        @Override
        public void onCloseButtonClicked(AjaxRequestTarget target) {
            super.onCloseButtonClicked(target);
            FileBrowserButton.this.onCancelClicked(target);
        }

        @Override
        protected void onCancelClicked(AjaxRequestTarget target) {
            super.onCancelClicked(target);
            FileBrowserButton.this.onCancelClicked(target);
            close(target);
        }

        @Override
        protected void onOkClicked(AjaxRequestTarget target) {
            super.onOkClicked(target);
            FileBrowserButton.this.onOkClicked(target);
            close(target);
        }
    }
}
