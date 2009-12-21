package org.artifactory.webapp.wicket.panel.advanced;

import com.google.common.collect.Lists;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Eli Givoni
 */
public class AdvancedSearchPanel extends WhiteTitlePanel {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(AdvancedSearchPanel.class);

    @SpringBean
    private RepositoryService repoService;


    public AdvancedSearchPanel(String id, IModel model) {
        super(id, model);
        add(new CssClass("advanced-search-panel"));
        add(new CollapsibleBehavior().setResizeModal(true));

        List<String> repoList = getOrderdRepoKeys();
        ListMultipleChoice choice = new ListMultipleChoice("selectedRepoForSearch", repoList);
        add(choice);
    }

    @Override
    protected Component newToolbar(String id) {
        return new HelpBubble(id, new ResourceModel("advancedHelp"));
    }

    private List<String> getOrderdRepoKeys() {
        List<RepoDescriptor> repoSet = Lists.newArrayList();
        repoSet.addAll(repoService.getLocalAndCachedRepoDescriptors());
        List<String> repoKeys = Lists.newArrayList();
        for (RepoDescriptor descriptor : repoSet) {
            repoKeys.add(descriptor.getKey());
        }
        return repoKeys;
    }
}
