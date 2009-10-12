package org.artifactory.webapp.wicket.page.browse.simplebrowser;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import static org.artifactory.webapp.wicket.utils.WebUtils.getWicketServletContextUrl;

/**
 * @author Yoav Aharoni
 */
public class BreadCrumbsPanel extends Panel {
    public BreadCrumbsPanel(String id, String path) {
        super(id);
        add(new CssClass("bread-crumbs"));

        RepeatingView items = new RepeatingView("item");
        add(items);

        String[] folders = path.split("[:/]");
        StringBuilder url = new StringBuilder(getWicketServletContextUrl());
        url.append("/");

        // add repo root
        String repo = folders[0];
        url.append(repo);
        url.append("/");
        items.add(new BreadCrumbItem(items.newChildId(), repo, url, ":"));

        for (int i = 1; i < folders.length; i++) {
            String folder = folders[i];
            url.append(folder);
            url.append("/");
            items.add(new BreadCrumbItem(items.newChildId(), folder, url, "/"));
        }
    }

    private class BreadCrumbItem extends WebMarkupContainer {
        private BreadCrumbItem(String id, String label, StringBuilder href, String sep) {
            super(id);
            add(new ExternalLink("link", href.toString(), label));
            add(new Label("sep", sep).setRenderBodyOnly(true));
        }
    }
}
