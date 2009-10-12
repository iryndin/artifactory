package org.artifactory.webapp.wicket.common.component.panel.links;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.ResourceModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;

/**
 * @author Yoav Aharoni
 */
public class LinksPanel extends Panel {
    private RepeatingView repeatingView;
    public static final String LINK_ID = "link";

    public LinksPanel(String id) {
        super(id);

        repeatingView = new RepeatingView("item");
        add(repeatingView);
    }

    public LinksPanel addLink(AbstractLink link) {
        WebMarkupContainer item = new WebMarkupContainer(repeatingView.newChildId());
        repeatingView.add(item);
        item.add(link);

        return this;
    }

    public LinksPanel addLinkFor(final Class<? extends Page> pageClass) {
        WebMarkupContainer item = new WebMarkupContainer(repeatingView.newChildId());
        repeatingView.add(item);

        String className = pageClass.getSimpleName();
        ResourceModel linkTitleModel = new ResourceModel(className, className);
        AbstractLink link = new TitledAjaxLink(LINK_ID, linkTitleModel) {
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(pageClass);
            }
        };
        item.add(link);

        return this;
    }

    public LinksPanel addSeperator() {
        WebMarkupContainer item = new WebMarkupContainer(repeatingView.newChildId());
        item.add(new WebMarkupContainer(LINK_ID).setVisible(false));
        item.add(new CssClass("sep"));
        repeatingView.add(item);

        return this;
    }
}
