package org.artifactory.webapp.wicket.common.component.panel.titled;

import static org.apache.commons.lang.StringUtils.isEmpty;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.artifactory.webapp.wicket.common.Titled;

/**
 * @author Yoav Aharoni
 */
public class TitleLabel extends Label {
    public TitleLabel(final Titled titled) {
        super("title", new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                return titled.getTitle();
            }
        });

    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && !isEmpty(getModelObjectAsString());
    }
}
