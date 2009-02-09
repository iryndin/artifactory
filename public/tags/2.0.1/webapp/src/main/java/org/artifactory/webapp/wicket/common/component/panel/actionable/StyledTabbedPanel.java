package org.artifactory.webapp.wicket.common.component.panel.actionable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.template.HtmlTemplate;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class StyledTabbedPanel extends AjaxTabbedPanel {
    public StyledTabbedPanel(String id, List tabs) {
        super(id, tabs);

        add(HeaderContributor.forJavaScript(StyledTabbedPanel.class, "StyledTabbedPanel.js"));
        add(new CssClass("styled-tab-panel"));
        Component tabsContainer = get("tabs-container");
        tabsContainer.setOutputMarkupId(true);

        ScrollLink moveLeft = new ScrollLink("moveLeft");
        add(moveLeft);

        ScrollLink moveRight = new ScrollLink("moveRight");
        add(moveRight);

        HtmlTemplate initScript = new HtmlTemplate("initScript");
        add(initScript);

        initScript.setParameter("tabsContainerId", new PropertyModel(tabsContainer, "markupId"));
        initScript.setParameter("moveLeftId", new PropertyModel(moveLeft, "markupId"));
        initScript.setParameter("moveRightId", new PropertyModel(moveRight, "markupId"));
    }

    private static class ScrollLink extends WebMarkupContainer {
        private ScrollLink(String id) {
            super(id);
            setOutputMarkupId(true);
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            tag.put("href", "#");
        }
    }
}
