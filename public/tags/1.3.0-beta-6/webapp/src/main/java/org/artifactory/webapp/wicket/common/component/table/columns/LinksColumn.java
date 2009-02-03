package org.artifactory.webapp.wicket.common.component.table.columns;

import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import static org.artifactory.webapp.wicket.common.component.panel.links.LinksPanel.LINK_ID;
import org.artifactory.webapp.wicket.common.component.table.SortableTable;
import org.artifactory.webapp.wicket.common.component.table.columns.panel.LinksColumnPanel;

import java.util.Collection;

/**
 * @author Yoav Aharoni
 */
public abstract class LinksColumn<T> extends AbstractColumn implements AttachColumnListener {
    protected LinksColumn() {
        this(new Model(""));
    }

    protected LinksColumn(String title) {
        this(new Model(title));
    }

    protected LinksColumn(IModel titleModel) {
        super(titleModel);
    }

    @SuppressWarnings({"unchecked"})
    public void populateItem(Item cellItem, String componentId, IModel rowModel) {
        cellItem.add(new CssClass("actions"));

        LinksColumnPanel panel = new LinksColumnPanel(componentId);
        cellItem.add(panel);

        Collection<? extends AbstractLink> links = getLinks((T) rowModel.getObject(), LINK_ID);
        for (AbstractLink link : links) {
            panel.addLink(link);
        }
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    public String getCssClass() {
        return LinksColumn.class.getSimpleName();
    }

    protected abstract Collection<? extends AbstractLink> getLinks(T rowObject, String linkId);

    public void onColumnAttached(SortableTable table) {
        table.add(new CssClass("selectable"));
    }
}
