package org.artifactory.webapp.wicket.panel.advanced;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.component.PlaceHolder;
import org.artifactory.common.wicket.component.panel.titled.TitleLabel;
import org.artifactory.common.wicket.model.Titled;

/**
 * @author Eli Givoni
 */
public class WhiteTitlePanel extends Panel implements Titled {
    protected static final String TITLE_KEY = "panel.title";

    public WhiteTitlePanel(String id) {
        super(id);
        init();
    }

    public WhiteTitlePanel(String id, IModel model) {
        super(id, model);
        init();
    }


    private void init() {
        setOutputMarkupId(true);
        add(new TitleLabel(this));
        add(newToolbar("tool"));
    }

    public String getTitle() {
        return getString(TITLE_KEY, null);
    }

    protected Component newToolbar(String id) {
        return new PlaceHolder(id);
    }
}
