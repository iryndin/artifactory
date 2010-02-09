package org.artifactory.webapp.wicket.application.sitemap;

/**
 * @author Yoav Aharoni
 */
public interface PageVisitor {
    void visit(PageNode node);
}
