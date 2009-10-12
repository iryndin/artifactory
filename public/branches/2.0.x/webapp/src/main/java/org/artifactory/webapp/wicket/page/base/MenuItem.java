package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.application.sitemap.MenuNode;
import org.artifactory.webapp.wicket.application.sitemap.SiteMap;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import static org.artifactory.webapp.wicket.page.base.MenuItem.Status.*;

/**
 * @author Yoav Aharoni
 */
public class MenuItem extends Panel {
    private Status status;

    public MenuItem(String id, MenuNode menuNode, Class<? extends Page> currentPage) {
        super(id);
        Class<? extends Page> pageClass = menuNode.getPageClass();
        boolean enabled = menuNode.isEnabled();
        status = fetchStatus(menuNode, currentPage, enabled);

        // add link
        WebMarkupContainer link;
        if (pageClass == null) {
            link = new ToogleGroupLink("link", menuNode);
        } else {
            link = new BookmarkablePageLink("link", pageClass);
            add(new CssClass(pageClass.getSimpleName()));
        }
        link.setEnabled(enabled);
        add(link);

        // add label
        link.add(new Label("name", menuNode.getName()));

        // add css
        String nodeType = menuNode.isLeaf() ? "menu-item" : "menu-group";
        String cssClass = nodeType + " " + nodeType + "-" + status.getCssClass();
        add(new CssClass(cssClass));
        link.add(new CssClass(status.getCssClass()));
    }

    public Status getStatus() {
        return status;
    }

    private Status fetchStatus(MenuNode node, Class<? extends Page> currentPage, boolean enabled) {
        if (!enabled) {
            return DISABLED;
        }

        if (currentPage.equals(node.getPageClass())) {
            return SELECTED;
        }

        Boolean opened = node.isOpened();
        if (Boolean.TRUE.equals(opened)) {
            return OPENED;
        } else if (Boolean.FALSE.equals(opened)) {
            return ENABLED;
        }

        SiteMap siteMap = ArtifactoryApplication.get().getSiteMap();
        MenuNode current = siteMap.getPageNode(currentPage);
        while (current != null) {
            if (current.equals(node)) {
                return OPENED;
            }
            current = current.getParent();
        }
        return ENABLED;
    }

    public enum Status {
        DISABLED("disabled"),
        ENABLED("enabled"),
        OPENED("opened"),
        SELECTED("selected");

        private String cssClass;

        Status(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }

        public boolean isOpened() {
            return equals(OPENED) || equals(SELECTED);
        }
    }

    private static class ToogleGroupLink extends WebMarkupContainer {
        private MenuNode menuNode;

        private ToogleGroupLink(String id, MenuNode menuNode) {
            super(id);
            this.menuNode = menuNode;
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            tag.put("href", "#");
            tag.put("onclick", "return SubMenuPanel.toogleMenu('" + menuNode.getCookieName() + "', this);");
        }
    }
}
