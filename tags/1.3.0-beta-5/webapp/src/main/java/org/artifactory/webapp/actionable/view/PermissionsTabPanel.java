/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable.view;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.actionable.model.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.component.CheckboxColumn;
import org.artifactory.webapp.wicket.security.acl.AceInfoRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays file statistics information.
 *
 * @author Yossi Shaul
 */
public class PermissionsTabPanel extends Panel {
    @SpringBean
    private AuthorizationService authService;

    @SpringBean
    private UserGroupService userGroupService;

    private RepoPath repoPath;

    public PermissionsTabPanel(String id, RepoAwareActionableItem item) {
        super(id);

        repoPath = item.getRepoPath();

        addTable();
    }

    private void addTable() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model("Principal"), "principal", "principal") {
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                super.populateItem(item, componentId, model);
                AceInfoRow aceInfoRow = (AceInfoRow) model.getObject();
                if (aceInfoRow.isGroup()) {
                    item.add(new CssClass("group"));
                } else {
                    item.add(new CssClass("user"));
                }
            }
        });

        columns.add(new DisabledCheckboxColumn("Delete", "delete", "delete", this));
        columns.add(new DisabledCheckboxColumn("Deploy", "deploy", "deploy", this));
        columns.add(new DisabledCheckboxColumn("Read", "read", "read", this));

        PermissionsTabTableDataProvider dataProvider =
                new PermissionsTabTableDataProvider(userGroupService, authService, repoPath);
        AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("recipients", columns, dataProvider, 10);

        add(table);
    }


    private class DisabledCheckboxColumn extends CheckboxColumn {
        public DisabledCheckboxColumn(String title, String expression, String sortProperty,
                WebMarkupContainer container) {
            super(title, expression, sortProperty, container);
        }

        @Override
        protected boolean isEnabled(Object row) {
            return false;
        }

        @Override
        protected void doUpdate(Object row, boolean checked, AjaxRequestTarget target) {
            // not updatable
        }
    }
}