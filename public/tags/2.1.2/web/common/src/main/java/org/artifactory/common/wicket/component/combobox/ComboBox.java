/*
 * This file is part of Artifactory.
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

package org.artifactory.common.wicket.component.combobox;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.behavior.combobox.ComboBoxBehavior;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ComboBox extends DropDownChoice {
    public ComboBox(String id) {
        super(id);
    }

    public ComboBox(String id, List<String> choices) {
        super(id, choices);
    }

    public ComboBox(String id, IModel model, List<String> choices) {
        super(id, model, choices);
    }

    public ComboBox(String id, IModel choices) {
        super(id, choices);
    }

    public ComboBox(String id, IModel model, IModel choices) {
        super(id, model, choices);
    }

    @Override
    protected Object convertChoiceIdToChoice(String id) {
        return id;
    }

    @Override
    protected CharSequence getDefaultChoice(Object selected) {
        return "";
    }

    {
        setType(String.class);
        setChoiceRenderer(new StringChoiceRenderer());
        add(new ComboBoxBehavior());
    }

    private static class StringChoiceRenderer implements IChoiceRenderer {
        public Object getDisplayValue(Object object) {
            return object;
        }

        public String getIdValue(Object object, int index) {
            return object == null ? null : object.toString();
        }
    }
}
