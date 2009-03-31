package org.artifactory.webapp.wicket.common.component.table.columns;

import org.artifactory.webapp.wicket.common.component.table.SortableTable;

/**
 * @author Yoav Aharoni
 */
public interface AttachColumnListener {
    void onColumnAttached(SortableTable table);
}
