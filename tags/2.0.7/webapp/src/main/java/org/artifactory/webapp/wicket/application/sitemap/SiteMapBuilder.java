package org.artifactory.webapp.wicket.application.sitemap;

/**
 * @author Yoav Aharoni
 */
public abstract class SiteMapBuilder {
    private SiteMap siteMap;

    public SiteMap getSiteMap() {
        return siteMap;
    }

    public void createSiteMap() {
        siteMap = new SiteMap();
    }

    public void cachePageNodes() {
        siteMap.cachePageNodes();
    }

    public abstract void buildSiteMap();
}
