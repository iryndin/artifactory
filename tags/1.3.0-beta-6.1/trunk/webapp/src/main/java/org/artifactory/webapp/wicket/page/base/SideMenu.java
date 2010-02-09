package org.artifactory.webapp.wicket.page.base;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.artifactory.webapp.wicket.application.sitemap.PageNode;
import static org.artifactory.webapp.wicket.page.base.MenuItem.Status.CHILD_SELECTED;
import static org.artifactory.webapp.wicket.page.base.MenuItem.Status.SELECTED;

import java.util.List;

/**
 * @author valdimiry
 */
public class SideMenu extends Panel {
    SideMenu(String id, List<PageNode> pages, Class<? extends BasePage> currentPage) {
        super(id);
        RepeatingView menu = new RepeatingView("menuItem");

        for (PageNode page : pages) {
            MenuItem menuItem = new MenuItem(menu.newChildId(), page, currentPage);
            menu.add(menuItem);
            if ((SELECTED.equals(menuItem.getStatus())
                    || CHILD_SELECTED.equals(menuItem.getStatus()))
                    && !page.getChildren().isEmpty()) {
                menu.add(new SideMenu(menu.newChildId(), page.getChildren(), currentPage));
            }
        }
        add(menu);
        if (pages.isEmpty()) {
            setVisible(false);
        }
    }

}
