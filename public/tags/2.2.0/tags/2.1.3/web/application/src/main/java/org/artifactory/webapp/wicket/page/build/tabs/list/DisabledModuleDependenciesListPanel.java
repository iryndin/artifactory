package org.artifactory.webapp.wicket.page.build.tabs.list;

import com.google.common.collect.Lists;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.build.api.Dependency;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleDependencyActionableItem;
import org.slf4j.Logger;

import java.util.List;

/**
 * The disabled modules dependencies list panel
 *
 * @author Noam Y. Tenne
 */
public class DisabledModuleDependenciesListPanel extends BaseModuleDependenciesListPanel {

    private static final Logger log = LoggerFactory.getLogger(DisabledModuleDependenciesListPanel.class);

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public DisabledModuleDependenciesListPanel(String id) {
        super(id);

        try {
            addTable();
        } catch (RepositoryRuntimeException rre) {
            String errorMessage = "An error occurred while loading the produced dependency list";
            log.error(errorMessage, rre);
            error(errorMessage);
        }
    }

    @Override
    protected List<Dependency> getDependencies() {
        return Lists.newArrayList();
    }

    @Override
    protected List<ModuleDependencyActionableItem> getModuleDependencyActionableItem(List<Dependency> dependencies) {
        return Lists.newArrayList();
    }
}