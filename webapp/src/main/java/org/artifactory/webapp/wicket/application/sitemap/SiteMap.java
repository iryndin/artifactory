package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.wicket.Page;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class SiteMap {
    private Map<Class<? extends Page>, PageNode> pagesCache = new HashMap<Class<? extends Page>, PageNode>();
    private PageNode root;

    public Collection<PageNode> getPages() {
        return pagesCache.values();
    }

    public PageNode getPageNode(Class<? extends Page> pageClass) {
        return pagesCache.get(pageClass);
    }

    public PageNode getRoot() {
        return root;
    }

    public void setRoot(PageNode root) {
        this.root = root;
    }

    public void visitPageNodes(PageNode node, PageVisitor visitor) {
        if (node == null) {
            return;
        }
        visitor.visit(node);
        for (PageNode child : node.getChildren()) {
            visitPageNodes(child, visitor);
        }
    }

    public void visitPageNodes(PageVisitor visitor) {
        visitPageNodes(getRoot(), visitor);
    }

    public void cachePageNodes() {
        visitPageNodes(new PageVisitor() {
            public void visit(PageNode node) {
                pagesCache.put(node.getPageClass(), node);
            }
        });
    }
}
