package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.wicket.Page;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class SiteMap {
    private Map<Class<? extends Page>, MenuNode> pagesCache = new HashMap<Class<? extends Page>, MenuNode>();
    private MenuNode root;

    public Collection<MenuNode> getPages() {
        return pagesCache.values();
    }

    public MenuNode getPageNode(Class<? extends Page> pageClass) {
        return pagesCache.get(pageClass);
    }

    public MenuNode getRoot() {
        return root;
    }

    public void setRoot(MenuNode root) {
        this.root = root;
    }

    public void visitPageNodes(MenuNode node, MenuNodeVisitor visitor) {
        if (node == null) {
            return;
        }
        visitor.visit(node);
        for (MenuNode child : node.getChildren()) {
            visitPageNodes(child, visitor);
        }
    }

    public void visitPageNodes(MenuNodeVisitor visitor) {
        visitPageNodes(getRoot(), visitor);
    }

    public void cachePageNodes() {
        visitPageNodes(new MenuNodeVisitor() {
            public void visit(MenuNode node) {
                Class<? extends Page> pageClass = node.getPageClass();
                if (pageClass != null) {
                    pagesCache.put(pageClass, node);
                }
            }
        });
    }
}
