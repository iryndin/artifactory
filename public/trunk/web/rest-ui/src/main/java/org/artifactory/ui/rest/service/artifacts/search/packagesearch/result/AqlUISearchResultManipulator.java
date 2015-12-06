package org.artifactory.ui.rest.service.artifacts.search.packagesearch.result;

import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;

/**
 * @author Dan Feldman
 */
public interface AqlUISearchResultManipulator {

    void manipulate(PackageSearchResult result);
}
