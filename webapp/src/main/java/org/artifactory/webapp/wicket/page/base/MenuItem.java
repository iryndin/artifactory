package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.application.sitemap.PageNode;
import org.artifactory.webapp.wicket.application.sitemap.SiteMap;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import static org.artifactory.webapp.wicket.page.base.MenuItem.Status.*;

/**
 * @author Yoav Aharoni
 */
public class MenuItem extends Panel {
    private Status status;

    public MenuItem(String id, PageNode pageNode, Class<? extends Page> currentPage) {
        super(id);
        Class<? extends Page> pageClass = pageNode.getPageClass();
        boolean enabled = pageNode.isEnabled();
        status = getStatus(pageNode, currentPage, enabled);

        // add link
        BookmarkablePageLink link = new BookmarkablePageLink("link", pageClass);
        link.setEnabled(enabled);
        add(link);

        // add label
        link.add(new Label("name", pageNode.getName()));

        // add css
        add(new CssClass(pageClass.getSimpleName()));
        String nodeType = pageNode.getChildren().isEmpty() ? "menu-item" : "menu-group";
        String cssClass = nodeType + " " + nodeType + "-" + status.getCssClass();
        add(new CssClass(cssClass));
        link.add(new CssClass(status.getCssClass()));
    }

    public Status getStatus() {
        return status;
    }

    private Status getStatus(PageNode pageNode, Class<? extends Page> currentPage, boolean enabled) {
        if (!enabled) {
            return DISABLED;
        }

        if (currentPage.equals(pageNode.getPageClass())) {
            return SELECTED;
        }

        SiteMap siteMap = ArtifactoryApplication.get().getSiteMap();
        PageNode current = siteMap.getPageNode(currentPage);
        while (current != null) {
            if (current.equals(pageNode)) {
                return CHILD_SELECTED;
            }
            current = current.getParent();
        }
        return ENABLED;
    }

    public enum Status {
        DISABLED("disabled"),
        ENABLED("enabled"),
        SELECTED("selected"),
        CHILD_SELECTED("child-selected");

        private String cssClass;

        Status(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

}
