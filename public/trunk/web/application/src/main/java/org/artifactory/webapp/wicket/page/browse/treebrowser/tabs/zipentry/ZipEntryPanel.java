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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.zipentry;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.tree.fs.ZipEntryInfo;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;

/**
 * Displays general item information. Placed inside the general info panel.
 *
 * @author Yossi Shaul
 */
public class ZipEntryPanel extends Panel {

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private CentralConfigService centralConfigService;

    @SpringBean
    private RepositoryService repositoryService;

    public ZipEntryPanel(String id, ZipEntryInfo zipEntry) {
        super(id);

        FieldSetBorder infoBorder = new FieldSetBorder("infoBorder");
        add(infoBorder);

        infoBorder.add(new LabeledValue("name", "Name: ", zipEntry.getName()));
        infoBorder.add(new LabeledValue("size", "Size: ", zipEntry.getSize() + ""));
        infoBorder.add(new LabeledValue("compressedSize", "Compressed Size: ", zipEntry.getCompressedSize() + ""));
        infoBorder.add(new LabeledValue("time", "Modification Time: ", zipEntry.getTime() + ""));
        infoBorder.add(new LabeledValue("crc", "CRC: ", zipEntry.getCrc() + ""));
        infoBorder.add(new LabeledValue("comment", "Comment: ", zipEntry.getComment()));
    }

}