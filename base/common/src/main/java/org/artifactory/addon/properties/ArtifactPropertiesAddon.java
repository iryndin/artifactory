package org.artifactory.addon.properties;

import org.artifactory.addon.Addon;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.repo.RepoPath;

import java.util.ArrayList;

/**
 * @author Chen Keinan
 */
public interface ArtifactPropertiesAddon extends Addon {

    void addPropertySha256RecursivelyMultiple(RepoPath repoPath);

    /**
     * update property control search
     *
     * @return property control search
     */
    PropertySearchControls getSha256PropertyControlSearch(String sha256, ArrayList<String> reposToSearch);
}
