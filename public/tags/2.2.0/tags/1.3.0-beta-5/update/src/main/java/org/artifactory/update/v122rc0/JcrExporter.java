package org.artifactory.update.v122rc0;

import org.artifactory.api.config.ImportableExportable;

import java.util.List;

/**
 * An implementation of this interface can export repositories content and metadata from a jcr.
 *
 * @author Yossi Shaul
 */
public interface JcrExporter extends ImportableExportable {
    /**
     * A list of repositories to export. If not set the default is all.
     *
     * @param reposToExport List of repositories keys to export.
     */
    void setRepositoriesToExport(List<String> reposToExport);

    /**
     * By default we don't export caches. This method allows us to change it
     *
     * @param includeCaches True to also export cached repositories.
     */
    void setIncludeCaches(boolean includeCaches);
}
