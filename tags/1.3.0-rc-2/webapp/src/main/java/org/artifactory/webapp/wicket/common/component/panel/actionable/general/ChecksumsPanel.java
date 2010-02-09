package org.artifactory.webapp.wicket.common.component.panel.actionable.general;

import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.webapp.wicket.common.component.LabeledValue;
import org.artifactory.webapp.wicket.common.component.border.fieldset.FieldSetBorder;

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
