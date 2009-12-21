package org.artifactory.webapp.wicket.page.build.tabs;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildService;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;

/**
 * Displays the build's XML representation
 *
 * @author Noam Y. Tenne
 */
public class BuildInfoXmlTabPanel extends Panel {

    @SpringBean
    private BuildService buildService;

    /**
     * Main constructor
     *
     * @param id    ID to assign to the panel
     * @param build Build to display
     */
    public BuildInfoXmlTabPanel(String id, Build build) {
        super(id);

        FieldSetBorder border = new FieldSetBorder("xmlBorder");
        add(border);

        String buildXml = buildService.getXmlFromBuild(build);

        TextContentPanel contentPanel = new TextContentPanel("xmlContent");
        contentPanel.setContent(buildXml);
        border.add(contentPanel);
    }
}