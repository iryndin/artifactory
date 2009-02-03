package org.artifactory.webapp.wicket.component.file.browser.panel;

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

    private PathAutoCompleteTextField fileAutocomplete;
    private DropDownChoice breadcrumbs;
    private ListView filesList;

    public FileBrowserPanel(String id, String root) {
        this(id, null, root, PathMask.BOTH);
    }

    public FileBrowserPanel(String id, String root, PathMask mask) {
        this(id, null, root, mask);
    }

    public FileBrowserPanel(String id, IModel model, String root) {
        this(id, model, root, PathMask.BOTH);
    }

    public FileBrowserPanel(String id, IModel model, String root, PathMask mask) {
        this(id, model, new PathHelper(root), mask);
    }

    public FileBrowserPanel(String id, IModel model, PathHelper pathHelper, PathMask mask) {
        super(id);

        if (model != null) {
            setModel(model);
        }

        this.pathHelper = pathHelper;

        init(mask);
    }

    protected void init(PathMask mask) {
        setOutputMarkupId(true);
        add(new SimpleAttributeModifier("class", "fileBrowser"));

        add(HeaderContributor.forCss(FileBrowserPanel.class, "style/filebrowser.css"));
        add(HeaderContributor.forJavaScript(FileBrowserPanel.class, "FileBrowserPanel.js"));

        // add component
        fileAutocomplete = new BrowserAutoCompleteTextField("fileAutocomplete", mask);
        add(fileAutocomplete);

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
                FileBrowserPanel.this.setModelObject(file);
                onOkClicked(target);
            } else {
                setRoot(file.getAbsolutePath());
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
        return fileAutocomplete.getMask();
    }

    public void setMask(PathMask mask) {
        fileAutocomplete.setMask(mask);
    }

    public String getRoot() {
        return pathHelper.getRoot();
    }

    public void setRoot(String root) {
        pathHelper.setRoot(root);
        filesList.setModelObject(pathHelper.getFiles("/", getMask()));
        breadcrumbs.setModelObject(new File(getRoot()));
    }

    private class DelegetedModelModel implements IModel {
        public Object getObject() {
            return FileBrowserPanel.this.getModelObject();
        }

        public void setObject(Object object) {
            FileBrowserPanel.this.setModelObject(object);
        }

        public void detach() {
        }
    }

    private static class OkButton extends WebComponent {
        public OkButton(String id) {
            super(id);
            add(new SimpleAttributeModifier("onclick", "FileBrowser.get().ok();"));
        }
    }

    private class CancelButton extends AjaxLink {
        public CancelButton(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            FileBrowserPanel.this.setModelObject(null);
            onCancelClicked(target);
        }
    }

    private class GotoParentButton extends AjaxLink {
        String rootPath = new File(pathHelper.getRootDir()).getAbsolutePath();

        public GotoParentButton(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            File currentFile = new File(getRoot());
            File parentFile = currentFile.getParentFile();
            if (parentFile == null || rootPath.equals(currentFile.getAbsolutePath())) {
                return;
            }

            setRoot(parentFile.getAbsolutePath());
            FileBrowserPanel.this.setModelObject(parentFile);
            target.addComponent(FileBrowserPanel.this);
        }
    }

    private class FilesListView extends ListView {
        public FilesListView(String id) {
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
                        setRoot(file.getAbsolutePath());
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
            return "new FileBrowser('" + FileBrowserPanel.this.getMarkupId() + "', '" +
                    fileAutocomplete.getMarkupId() + "');";
        }
    }

    private class BreadCrumbsDropDownChoice extends DropDownChoice {
        public BreadCrumbsDropDownChoice(String id) {
            super(id, new Model(new File(getRoot())), new BreadCrumbsModel(),
                    new ChoiceRenderer("name", "absolutePath"));

            add(new AjaxFormComponentUpdatingBehavior("onchange") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    File root = (File) getModelObject();
                    setRoot(root.getAbsolutePath());
                    FileBrowserPanel.this.setModelObject(root);

                    target.addComponent(FileBrowserPanel.this);
                }
            });
        }

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

            buffer.append("style=\"padding-left: " + (index * 10 + 18) +
                    "px; background-position: " + (index * 10) + "px\" ");
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
            String rootPath = new File(pathHelper.getRootDir()).getAbsolutePath();
            for (File folder = new File(getRoot()); folder != null;
                 folder = folder.getParentFile()) {
                if (rootPath.equals(folder.getAbsolutePath())) {
                    breadCrumbs.add(0, folder);
                    break;
                }
                breadCrumbs.add(0, folder);
            }
            return breadCrumbs;
        }
    }

    private class BrowserAutoCompleteTextField extends PathAutoCompleteTextField {
        public BrowserAutoCompleteTextField(String id, PathMask mask) {
            super(id, new DelegetedModelModel(), FileBrowserPanel.this.pathHelper, mask);

            add(new AjaxFormComponentUpdatingBehavior("onselection") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    File file = (File) fileAutocomplete.getModelObject();
                    onFileSelected(file, target);
                }
            });

        }
    }
}
