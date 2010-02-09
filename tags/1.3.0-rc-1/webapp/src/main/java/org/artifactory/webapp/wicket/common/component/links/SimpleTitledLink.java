package org.artifactory.webapp.wicket.common.component.links;

/**
 * @author Yoav Aharoni
 */
public class SimpleTitledLink extends TitledLink {
    public SimpleTitledLink(String id, String title) {
        super(id, title);
        setOutputMarkupId(true);
    }

    @Override
    public void onClick() {
    }
}
