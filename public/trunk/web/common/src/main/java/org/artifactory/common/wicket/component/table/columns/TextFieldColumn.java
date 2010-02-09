/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.common.wicket.component.table.columns;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.table.columns.panel.textfield.TextFieldPanel;

/**
 * @author Yoav Aharoni
 */
public class TextFieldColumn<T> extends AbstractColumn {
    private String expression;

    public TextFieldColumn(String title, String expression, String sortProperty) {
        super(new Model(title), sortProperty);
        this.expression = expression;
    }

    public void populateItem(final Item cellItem, String componentId, final IModel rowModel) {
        T rowObject = getRowModelObject(rowModel);

        MarkupContainer panel = new TextFieldPanel(componentId, rowModel);
        cellItem.add(new CssClass("TextFieldColumn"));
        cellItem.add(panel);

        IModel model = newPropertyModel(rowObject);
        FormComponent textField = newTextField(TextFieldPanel.TEXTFIELD_ID, model, rowObject);
        panel.add(textField);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected FormComponent newTextField(String id, IModel model, T rowObject) {
        return new TextField(id, model);
    }

    protected IModel newPropertyModel(T rowObject) {
        return new PropertyModel(rowObject, expression);
    }

    @SuppressWarnings({"unchecked"})
    protected final T getRowModelObject(IModel model) {
        return (T) model.getObject();
    }

    public final String getExpression() {
        return expression;
    }
}