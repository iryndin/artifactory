package org.artifactory.webapp.wicket.common.component.table.columns.panel.links;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.component.panel.links.LinksPanel;
import org.artifactory.webapp.wicket.common.component.template.HtmlTemplate;

/**
 * @author Yoav Aharoni
 */
public class LinksColumnPanel extends Panel {
    private LinksPanel linksPanel;

    public LinksColumnPanel(String id) {
        super(id);

        add(HeaderContributor.forJavaScript(LinksColumnPanel.class, "LinksColumnPanel.js"));

        linksPanel = new LinksPanel("links");
        linksPanel.setOutputMarkupId(true);
        add(linksPanel);

        WebComponent icon = new WebComponent("icon");
        icon.setOutputMarkupId(true);
        add(icon);

        HtmlTemplate initScript = new HtmlTemplate("initScript");
        initScript.setParameter("iconId", new PropertyModel(icon, "markupId"));
        initScript.setParameter("panelId", new PropertyModel(linksPanel, "markupId"));
        add(initScript);
    }

    public void addLink(AbstractLink link) {
        linksPanel.addLink(link);
    }
}
