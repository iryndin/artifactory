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

package org.artifactory.common.wicket.panel.editor;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;

/**
 * @author Noam Tenne
 */
public class TextEditorPanel extends TitledPanel {

    private TextArea editorTextArea;
    private String title;

    @WicketProperty
    private String editorValue;

    public TextEditorPanel(String id, String title, String helpMessage) {
        this(id, title, Model.of(helpMessage));
    }

    public TextEditorPanel(String id, String title, IModel helpModel) {
        super(id, helpModel);
        this.title = title;
        addTextArea();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    protected Component newToolbar(String id) {
        return new HelpBubble(id, getDefaultModel());
    }

    public void setEditorValue(String pomText) {
        editorValue = pomText;
    }

    public String getEditorValue() {
        return editorValue;
    }

    public void addTextAreaBehavior(AjaxFormComponentUpdatingBehavior behavior) {
        editorTextArea.add(behavior);
    }

    /**
     * Put the focus on the {@link TextArea} and place the caret inside
     */
    public void addTextAreaFocusBehaviour() {
        editorTextArea.add(new FocusBehavior());
    }

    private void addTextArea() {
        editorTextArea = new TextArea<String>("editorTextArea", newTextModel());
        add(editorTextArea);
    }

    /**
     * Behaviour to focus the window and place the caret inside a specific {@link Component}
     */
    private static class FocusBehavior extends AbstractBehavior {

        private Component component;

        @Override
        public void bind(Component component) {
            super.bind(component);
            this.component = component;
            component.setOutputMarkupId(true);
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.renderOnLoadJavascript("document.getElementById('" + component.getMarkupId() + "').focus();");
        }
    }

    protected IModel<String> newTextModel() {
        return new PropertyModel<String>(this, "editorValue");
    }
}
