package org.artifactory.ui.rest.service.artifacts.search.packagesearch.result;

import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;

/**
 * A result manipulator that does nothing just so we don't have to deal with null checks.
 *
 * @author Dan Feldman
 */
public class AqlUISearchDummyResultManipulator implements AqlUISearchResultManipulator {

    @Override
    public void manipulate(PackageSearchResult result) {
        //Dummy
    }
}
