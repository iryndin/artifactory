package org.artifactory.webapp.wicket.common.component.modal.panel.bordered;

import org.apache.wicket.Component;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;

/**
 * @author Yoav Aharoni
 */
public class BorderedModelPanel extends BaseModalPanel {
    public static final String CONTENT_ID = ModalHandler.CONTENT_ID;

    public BorderedModelPanel(Component content) {
        TitledBorder border = new TitledBorder("border");
        add(border);
        border.add(content);
    }
}
