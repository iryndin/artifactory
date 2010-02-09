package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build;

import com.google.common.collect.Lists;
import org.apache.wicket.Component;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable.BuildTabActionableItem;

import java.util.List;

/**
 * The disabled repo item build associaction tab panel
 *
 * @author Noam Y. Tenne
 */
public class DisabledBuildsTabPanel extends BaseBuildsTabPanel {

    @SpringBean
    private SearchService searchService;

    /**
     * Main constructor
     *
     * @param id   ID to assign to the panel
     * @param item Selected repo item
     */
    public DisabledBuildsTabPanel(String id, RepoAwareActionableItem item) {
        super(id, item);
        add(new CssClass("disabled-tab-panel"));

        setEnabled(false);
        visitChildren(new IVisitor() {
            public Object component(Component component) {
                component.setEnabled(false);
                return CONTINUE_TRAVERSAL;
            }
        });
    }

    @Override
    protected List<BuildTabActionableItem> getArtifactActionableItems() {
        return Lists.newArrayList();
    }

    @Override
    protected List<BuildTabActionableItem> getDependencyActionableItems() {
        return Lists.newArrayList();
    }
}