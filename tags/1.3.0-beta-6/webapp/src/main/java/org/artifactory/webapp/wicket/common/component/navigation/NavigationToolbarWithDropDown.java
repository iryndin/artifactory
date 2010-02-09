package org.artifactory.webapp.wicket.common.component.navigation;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.component.table.SortableTable;

/**
 * User: yevgenys
 * Date: Oct 16, 2008
 */
public class NavigationToolbarWithDropDown extends AbstractToolbar {

    public NavigationToolbarWithDropDown(SortableTable table) {
        super(table);

        WebMarkupContainer span = new WebMarkupContainer("span");
        add(span);
        span.add(new AttributeModifier("colspan", true, new Model(String
                .valueOf(table.getColumns().length))));

        span.add(newPagingNavigator("navigator", table));
        span.add(newNavigatorLabel("navigatorLabel"));

    }

    protected WebComponent newNavigatorLabel(String navigatorId) {
        IModel model = new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                return getNavigatorText();
            }
        };

        return new Label(navigatorId, model);
    }

    protected String getNavigatorText() {
        return getString("navigator.text", new Model(getTable()));
    }

    protected Panel newPagingNavigator(String navigatorId, DataTable table) {
        return new PagingNavigatorWithDropDown(navigatorId, table);
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && getTable().getPageCount() > 1;
    }
}
