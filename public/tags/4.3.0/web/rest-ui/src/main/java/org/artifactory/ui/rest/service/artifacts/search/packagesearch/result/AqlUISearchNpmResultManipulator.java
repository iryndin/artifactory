package org.artifactory.ui.rest.service.artifacts.search.packagesearch.result;

import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a scope field whenever a scoped package is being returned (name is @{scope}/{name})
 *
 * @author Dan Feldman
 */
public class AqlUISearchNpmResultManipulator implements AqlUISearchResultManipulator {
    private static final Logger log = LoggerFactory.getLogger(AqlUISearchNpmResultManipulator.class);

    @Override
    public void manipulate(PackageSearchResult result) {
        String npmName = result.getExtraFieldsMap().get(PackageSearchCriteria.npmName.name()).iterator().next();
        if (npmName.contains("@")) {
            try {
                String[] split = npmName.split("/");
                log.debug("Manipulator adding npm scope: '{}' and changing package name to '{}'", split[0], split[1]);
                result.getExtraFieldsMap().removeAll(PackageSearchCriteria.npmName.name());
                result.getExtraFieldsMap().put(PackageSearchCriteria.npmName.name(), split[1]);
                result.getExtraFieldsMap().put(PackageSearchCriteria.npmScope.name(), split[0]);
            } catch (Exception e) {
                log.warn("Error parsing npm package name: '{}", npmName);
            }
        }
    }
}
