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
import org.artifactory.common.wicket.behavior.filteringselect.FilteringSelectBehavior;

import java.util.List;

/**
 * A Dojo FilteringSelect widget.<br/>
 * <br/>
 * <b>NOTE!</b> You cannot add FilteringSelect to ajax target regularly,
 * meaning <b>you can't do <code>target.addComponent(dropDown)</code></b>.
 * Instead add a containing parent: <code>target.addComponent(anyParent)</code>
 * or pass getAjaxTargetMarkupId() as target markup id like so:
 * <code>target.addComponent(dropDown<b>, dropDown.getAjaxTargetMarkupId()</b>)</code>
 *
 * @author Yoav Aharoni
 * @see FilteringSelect#getAjaxTargetMarkupId()
 */
public class FilteringSelect extends DropDownChoice {
	public FilteringSelect(String id) {
		super(id);
	}

	public FilteringSelect(String id, List choices) {
		super(id, choices);
	}

	public FilteringSelect(String id, List data, IChoiceRenderer renderer) {
		super(id, data, renderer);
	}

	public FilteringSelect(String id, IModel model, List choices) {
		super(id, model, choices);
	}

	public FilteringSelect(String id, IModel model, List data, IChoiceRenderer renderer) {
		super(id, model, data, renderer);
	}

	public FilteringSelect(String id, IModel choices) {
		super(id, choices);
	}

	public FilteringSelect(String id, IModel model, IModel choices) {
		super(id, model, choices);
	}

	public FilteringSelect(String id, IModel choices, IChoiceRenderer renderer) {
		super(id, choices, renderer);
	}

	public FilteringSelect(String id, IModel model, IModel choices, IChoiceRenderer renderer) {
		super(id, model, choices, renderer);
	}

	{
		add(new FilteringSelectBehavior());
	}

	public String getAjaxTargetMarkupId() {
		return getMarkupId() + "-widget";
	}
}
