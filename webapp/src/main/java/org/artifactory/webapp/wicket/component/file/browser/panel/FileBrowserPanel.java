package org.artifactory.webapp.wicket.component.file.browser.panel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;
import org.artifactory.webapp.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.component.file.path.PathHelper;
import org.artifactory.webapp.wicket.component.file.path.PathMask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class FileBrowserPanel extends Panel {
    private PathHelper pathHelper;
    private String chRoot;

    private PathAutoCompleteTextField pathAutoCompleteTextField;
    private DropDownChoice breadcrumbs;
    private ListView filesList;


    public FileBrowserPanel(String id) {
        this(id, null, new PathHelper());
    }

    public FileBrowserPanel(String id, IModel model) {
        this(id, model, new PathHelper());
    }

    public FileBrowserPanel(String id, String root) {
        this(id, null, root);
    }

    public FileBrowserPanel(String id, IModel model, String root) {
        this(id, model, new PathHelper(root));
    }

    protected FileBrowserPanel(String id, IModel model, PathHelper pathHelper) {
        super(id);

        if (model != null) {
            setModel(model);
        }

        this.pathHelper = pathHelper;
        init();
    }

    protected void init() {
        setChRoot(pathHelper.getRoot());

        setOutputMarkupId(true);
        add(new SimpleAttributeModifier("class", "fileBrowser"));

        add(HeaderContributor.forCss(FileBrowserPanel.class, "style/filebrowser.css"));
        add(HeaderContributor.forJavaScript(FileBrowserPanel.class, "FileBrowserPanel.js"));

        // add component
        pathAutoCompleteTextField = new BrowserAutoCompleteTextField("fileAutocomplete");
        add(pathAutoCompleteTextField);

        breadcrumbs = new BreadCrumbsDropDownChoice("breadcrumbs");
        add(breadcrumbs);

        filesList = new FilesListView("filesList");
        add(filesList);

        // add init script
        Label label = new Label("initScript", new InitScriptModel());
        label.setEscapeModelStrings(false);
        add(label);

        // add  buttons
        add(new GotoParentButton("gotoParentButton"));
        add(new OkButton("ok"));
        add(new CancelButton("cancel"));
    }


    protected void onOkClicked(AjaxRequestTarget target) {
    }

    protected void onCancelClicked(AjaxRequestTarget target) {
    }

    protected void onFileSelected(File file, AjaxRequestTarget target) {
        if (file.isDirectory()) {
            // is folder
            if (getMask().includeFolders()) {
                setModelObject(file);
                onOkClicked(target);
            } else {
                setCurrentFolder(file.getAbsolutePath());
                target.addComponent(this);
            }
            return;
        }

        // is file
        setModelObject(file);
        onOkClicked(target);
    }

    public String getTitle() {
        return getString("file.browser.title", null);
    }

    public PathMask getMask() {
        return pathAutoCompleteTextField.getMask();
    }

    public void setMask(PathMask mask) {
        pathAutoCompleteTextField.setMask(mask);
    }

    public void setChRoot(String chRoot) {
        this.chRoot = new File(chRoot == null ? "/" : chRoot).getAbsolutePath();
    }

    public String getChRoot() {
        return chRoot;
    }

    public String getCurrentFolder() {
        return StringUtils.defaultString(pathHelper.getRoot(), "/");
    }

    public void setCurrentFolder(String root) {
        pathHelper.setRoot(root);
        filesList.setModelObject(pathHelper.getFiles("/", getMask()));
        breadcrumbs.setModelObject(new File(getCurrentFolder()));
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

    private static class OkButton extends WebComponent {
        private OkButton(String id) {
            super(id);
            add(new SimpleAttributeModifier("onclick", "FileBrowser.get().ok();"));
        }
    }

    private class CancelButton extends AjaxLink {
        private CancelButton(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            FileBrowserPanel.this.setModelObject(null);
            onCancelClicked(target);
        }
    }

    private class GotoParentButton extends AjaxLink {
        private GotoParentButton(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            File currentFile = new File(getCurrentFolder());
            File parentFile = currentFile.getParentFile();
            if (parentFile == null || chRoot.equals(currentFile.getAbsolutePath())) {
                return;
            }

            setCurrentFolder(parentFile.getAbsolutePath());
            FileBrowserPanel.this.setModelObject(parentFile);
            target.addComponent(FileBrowserPanel.this);
        }
    }

    private class FilesListView extends ListView {
        private FilesListView(String id) {
            super(id, pathHelper.getFiles("/", getMask()));
        }

        @Override
        protected void populateItem(ListItem item) {
            File file = (File) item.getModelObject();
            item.add(new FileLink("fileNameLabel", file));
            item.add(new SimpleAttributeModifier("class", file.isDirectory() ? "folder" : "file"));

        }
    }

    private class FileLink extends Label {
        private FileLink(String id, final File file) {
            super(id, file.getName());

            add(new SimpleAttributeModifier("onclick",
                    "FileBrowser.get().onFileClick(this, event);"));

            if (file.isDirectory()) {
                add(new AjaxEventBehavior("ondblclick") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        setCurrentFolder(file.getAbsolutePath());
                        FileBrowserPanel.this.setModelObject(file);
                        target.addComponent(FileBrowserPanel.this);
                    }
                });
            } else {
                add(new AjaxEventBehavior("ondblclick") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        onFileSelected(file, target);
                    }
                });
            }

        }
    }

    private class InitScriptModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return "new FileBrowser('" + getMarkupId() + "', '" +
                    pathAutoCompleteTextField.getMarkupId() + "');";
        }
    }

    private class BreadCrumbsDropDownChoice extends DropDownChoice {
        private BreadCrumbsDropDownChoice(String id) {
            super(id, new Model(new File(getCurrentFolder())), new BreadCrumbsModel(),
                    new ChoiceRenderer("name", "absolutePath"));

            add(new AjaxFormComponentUpdatingBehavior("onchange") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    File root = (File) getModelObject();
                    setCurrentFolder(root.getAbsolutePath());
                    FileBrowserPanel.this.setModelObject(root);

                    target.addComponent(FileBrowserPanel.this);
                }
            });
        }

        @SuppressWarnings({"RefusedBequest"})
        @Override
        protected void appendOptionHtml(AppendingStringBuffer buffer, Object choice, int index,
                                        String selected) {
            IChoiceRenderer renderer = getChoiceRenderer();
            Object objectValue = renderer.getDisplayValue(choice);
            String displayValue = "";
            if (objectValue != null) {
                displayValue = getConverter(objectValue.getClass())
                        .convertToString(objectValue, getLocale());
            }

            if ("".equals(displayValue)) {
                displayValue = getString("file.browser.breadCrumbs.root", null);
            }
            buffer.append("\n<option ");
            if (isSelected(choice, index, selected)) {
                buffer.append("selected=\"selected\" ");
            }

            buffer.append("style=\"padding-left: " + (index * 10 + 18) + "px; background-position: " + index * 10 + "px\" ");
            index++;

            buffer.append("value=\"");
            buffer.append(Strings.escapeMarkup(renderer.getIdValue(choice, index)));
            buffer.append("\">");

            CharSequence escaped = escapeOptionHtml(displayValue);
            buffer.append(escaped);
            buffer.append("</option>");
        }
    }

    private class BreadCrumbsModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            List<File> breadCrumbs = new ArrayList<File>();
            for (File folder = new File(getCurrentFolder()); folder != null;
                 folder = folder.getParentFile()) {
                if (chRoot.equals(folder.getAbsolutePath())) {
                    breadCrumbs.add(0, folder);
                    break;
                }
                breadCrumbs.add(0, folder);
            }
            return breadCrumbs;
        }
    }

    private class BrowserAutoCompleteTextField extends PathAutoCompleteTextField {
        private BrowserAutoCompleteTextField(String id) {
            super(id, new DelegetedModelModel(), FileBrowserPanel.this.pathHelper);

            add(new AjaxFormComponentUpdatingBehavior("onselection") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    File file = (File) pathAutoCompleteTextField.getModelObject();
                    onFileSelected(file, target);
                }
            });

        }
    }
}
