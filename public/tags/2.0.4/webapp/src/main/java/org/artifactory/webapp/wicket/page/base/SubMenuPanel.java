package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.artifactory.webapp.wicket.application.sitemap.MenuNode;
import org.artifactory.webapp.wicket.common.behavior.CssClass;

import java.util.List;

/**
 * @author valdimiry
 */
public class SubMenuPanel extends Panel {
    SubMenuPanel(String id, List<MenuNode> menuNodes, Class<? extends BasePage> currentPage) {
        super(id);
        add(HeaderContributor.forJavaScript(SubMenuPanel.class, "SubMenuPanel.js"));

        RepeatingView items = new RepeatingView("menuItem");
        add(items);

        for (MenuNode menuNode : menuNodes) {
            MenuItem menuItem = new MenuItem(items.newChildId(), menuNode, currentPage);
            items.add(menuItem);

            if (!menuNode.isLeaf()) {
                SubMenuPanel subMenuPanel = new SubMenuPanel(items.newChildId(), menuNode.getChildren(), currentPage);
                items.add(subMenuPanel);

                boolean opened = menuItem.getStatus().isOpened();
                String cssClass = opened ? "sub-menu-opened" : "sub-menu-closed";
                subMenuPanel.add(new CssClass(cssClass));
            }
        }

        if (menuNodes.isEmpty()) {
            setVisible(false);
        }
    }
}
