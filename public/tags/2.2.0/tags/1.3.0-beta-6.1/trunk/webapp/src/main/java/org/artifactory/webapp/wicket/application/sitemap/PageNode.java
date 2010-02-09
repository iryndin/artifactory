package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.wicket.Application;
import org.apache.wicket.Page;

import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class PageNode implements TreeNode, Serializable {
    private Class<? extends Page> pageClass;
    private String name;
    private String url;
    private PageNode parent;
    private List<PageNode> children = new ArrayList<PageNode>();

    public PageNode() {
    }

    public PageNode(Class<? extends Page> pageClass, String name) {
        this.pageClass = pageClass;
        this.name = name;
    }

    public Class<? extends Page> getPageClass() {
        return pageClass;
    }

    public void setPageClass(Class<? extends Page> pageClass) {
        this.pageClass = pageClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PageNode getParent() {
        return parent;
    }

    public void setParent(PageNode parent) {
        this.parent = parent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<PageNode> getChildren() {
        return children;
    }

    public void setChildren(List<PageNode> children) {
        this.children = children;
    }

    public void addChild(PageNode child) {
        child.setParent(this);
        children.add(child);
    }

    public void removeChild(PageNode child) {
        children.remove(child);
    }

    public boolean isEnabled() {
        return Application.get().getSecuritySettings().getAuthorizationStrategy().isInstantiationAuthorized(pageClass);
    }

    @SuppressWarnings({"ControlFlowStatementWithoutBraces", "RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageNode pageNode = (PageNode) o;

        if (!name.equals(pageNode.name)) return false;
        if (!pageClass.equals(pageNode.pageClass)) return false;
        if (parent != null ? !parent.equals(pageNode.parent) : pageNode.parent != null) return false;
        if (url != null ? !url.equals(pageNode.url) : pageNode.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = pageClass.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    public int getChildCount() {
        return children.size();
    }

    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public Enumeration children() {
        return new IteratorEnumeration(children.iterator());
    }
}
