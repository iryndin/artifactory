package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import static org.artifactory.webapp.wicket.common.util.CookieUtils.*;

import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class MenuNode implements TreeNode, Serializable {
    private Class<? extends Page> pageClass;
    private String name;
    private String url;
    private MenuNode parent;
    private List<MenuNode> children = new ArrayList<MenuNode>();
    private String cookieName;

    public MenuNode(String name) {
        this.name = name;
    }

    public MenuNode(String name, Class<? extends Page> pageClass) {
        this(name);
        this.pageClass = pageClass;
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

    public MenuNode getParent() {
        return parent;
    }

    public void setParent(MenuNode parent) {
        this.parent = parent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MenuNode> getChildren() {
        return children;
    }

    public void setChildren(List<MenuNode> children) {
        this.children = children;
    }

    public void addChild(MenuNode child) {
        child.setParent(this);
        children.add(child);
    }

    public void removeChild(MenuNode child) {
        children.remove(child);
    }

    public boolean isEnabled() {
        if (pageClass == null) {
            // sub menu, check if any child is enabled
            for (MenuNode child : children) {
                if (child.isEnabled()) {
                    return true;
                }
            }
            return false;
        }

        return getAuthorizationStrategy().isInstantiationAuthorized(pageClass);
    }

    @SuppressWarnings({"ControlFlowStatementWithoutBraces", "RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MenuNode pageNode = (MenuNode) o;

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

    @SuppressWarnings({"SuspiciousMethodCalls"})
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

    public String getCookieName() {
        if (cookieName == null) {
            cookieName = generateCookieName();
        }
        return cookieName;
    }

    private String generateCookieName() {
        if (parent == null) {
            return "menu";
        }
        return parent.getCookieName() + "." + parent.getIndex(this);
    }

    public Boolean isOpened() {
        if (isLeaf()) {
            return false;
        }

        String cookieValue = getCookie(getCookieName());
        if (StringUtils.isEmpty(cookieValue)) {
            return null;
        }

        return Boolean.TRUE.toString().equals(cookieValue);
    }

    public void setOpened(Boolean opened) {
        if (isLeaf()) {
            return;
        }

        if (opened == null) {
            clearCookie(getCookieName());
            return;
        }

        setCookie(getCookieName(), opened);
    }

    protected IAuthorizationStrategy getAuthorizationStrategy() {
        return Application.get().getSecuritySettings().getAuthorizationStrategy();
    }
}
