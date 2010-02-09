package org.artifactory.webapp.actionable.view;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class StyledTabbedPanel extends AjaxTabbedPanel {
    public StyledTabbedPanel(String id, List tabs) {
        super(id, tabs);
    }
}
