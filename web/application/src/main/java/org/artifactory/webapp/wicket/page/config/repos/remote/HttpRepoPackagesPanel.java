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

package org.artifactory.webapp.wicket.page.config.repos.remote;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.common.wicket.behavior.border.TitledBorderBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

/**
 * Packages configuration panel.
 *
 * @author Yossi Shaul
 */
public class HttpRepoPackagesPanel extends Panel {

    public HttpRepoPackagesPanel(String id) {
        super(id);

        addP2Fields();
    }

    private void addP2Fields() {
        WebMarkupContainer p2Border = new WebMarkupContainer("p2Border");
        p2Border.add(new TitledBorderBehavior("fieldset-border", "P2 Support"));
        add(p2Border);

        p2Border.add(new StyledCheckbox("p2Support"));
        p2Border.add(new SchemaHelpBubble("p2Support.help"));
    }
}
