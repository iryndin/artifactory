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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;

/**
 * A panel to display MD5 and SHA1 checksums on the GeneralTabPanel
 *
 * @author Noam Tenne
 */
public class ChecksumsPanel extends Panel {

    public ChecksumsPanel(String id, FileInfo file) {

        super(id);

        FieldSetBorder border = new FieldSetBorder("border");
        add(border);

        String md5 = "";
        String sha1 = "";

        //Make sure checksums are valid
        if (file.getMd5() != null) {
            md5 = file.getMd5();
        }

        if (file.getSha1() != null) {
            sha1 = file.getSha1();
        }
        border.add(new LabeledValue("md5", "MD5: ", md5));
        border.add(new LabeledValue("sha1", "SHA1: ", sha1));
    }

}
