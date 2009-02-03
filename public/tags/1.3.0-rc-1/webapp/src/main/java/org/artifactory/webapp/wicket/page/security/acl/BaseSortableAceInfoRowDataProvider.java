package org.artifactory.webapp.wicket.page.security.acl;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.utils.ListPropertySorter;

import java.util.Iterator;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public abstract class BaseSortableAceInfoRowDataProvider extends SortableDataProvider {
    protected List<AceInfoRow> aces;

    public BaseSortableAceInfoRowDataProvider() {
        setSort("principal", true);
    }

    public abstract void loadData();

    public Iterator iterator(int first, int count) {
        ListPropertySorter.sort(aces, getSort());
        List<AceInfoRow> list = aces.subList(first, first + count);
        return list.iterator();
    }

    public int size() {
        return aces.size();
    }

    public IModel model(Object object) {
        return new Model((AceInfoRow) object);
    }
}
