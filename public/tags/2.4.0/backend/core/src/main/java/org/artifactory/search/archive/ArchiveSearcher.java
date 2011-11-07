/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.search.archive;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.common.ConstantValues;
import org.artifactory.fs.FileInfo;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsQueryRow;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;
import org.jdom.Element;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;

import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_ARCHIVE_ENTRY;

/**
 * @author Noam Tenne
 */
public class ArchiveSearcher extends SearcherBase<ArchiveSearchControls, ArchiveSearchResult> {

    private static final Logger log = LoggerFactory.getLogger(ArchiveSearcher.class);

    private static final String HIGHLIGHT_TAG = "highlight";

    @Override
    public ItemSearchResults<ArchiveSearchResult> doSearch(ArchiveSearchControls controls) {
        String queryString = controls.getQuery();
        validateQueryLength(queryString);

        VfsRepoQuery query = createRepoQuery(controls);
        query.addAllSubPathFilter();
        query.setNodeTypeFilter(VfsNodeType.FILE);
        query.addCriterion(PROP_ARTIFACTORY_ARCHIVE_ENTRY, VfsComparatorType.CONTAINS, queryString);
        VfsQueryResult queryResult = query.execute(controls.isLimitSearchResults());

        List<ArchiveSearchResult> resultList = Lists.newArrayList();
        long fullResults = 0;
        Iterator<VfsQueryRow> rowIterator = queryResult.rowsIterator();
        PathFactory pathFactory = PathFactoryHolder.get();
        String escapedExp = pathFactory.escape(queryString);
        while (rowIterator.hasNext()) {
            VfsQueryRow row = rowIterator.next();
            try {
                String excerpt = row.excerpt(PROP_ARTIFACTORY_ARCHIVE_ENTRY);
                log.debug("Archive search excerpt result: {}", excerpt);
                List<ArchiveSearchEntry> entriesToInclude =
                        getContentSearchEntries(controls.isExcludeInnerClasses(), excerpt);

                /**
                 * If the list is null, skip the adding of the result since all it's content was excluded as opposed to
                 * a case of too many results
                 */
                if (entriesToInclude == null) {
                    continue;
                }

                int entriesSize = entriesToInclude.size();
                fullResults += (entriesSize == 0) ? 1 : entriesSize;

                //If the search results are limited, stop when reached more than max results + 1
                if (!controls.isLimitSearchResults() || (resultList.size() < getMaxResults())) {
                    String artifactPath = row.nodeAbsolutePath();
                    RepoPath repoPath = pathFactory.getRepoPath(artifactPath);
                    if (!isResultAcceptable(repoPath)) {
                        continue;
                    }

                    FileInfo fileInfo = VfsItemFactory.createFileInfoProxy(repoPath);
                    boolean shouldCalc = controls.shouldCalcEntries();
                    boolean entriesNotEmpty = (entriesSize != 0);

                    if (shouldCalc && entriesNotEmpty) {
                        /**
                         * Handle normal archive search (needs to calculate entry paths for display and results were
                         * returned)
                         */
                        for (ArchiveSearchEntry entry : entriesToInclude) {
                            String entryName = entry.getEntryName();
                            if (StringUtils.isEmpty(entryName)) {
                                entryName = escapedExp;
                            }
                            resultList.add(new ArchiveSearchResult(fileInfo, entryName, entry.getEntryPath(), true));
                        }
                    } else {
                        /**
                         * Create generic entries when we don't need to calculate paths (performing a search for the
                         * "saved search results") or if the search query was too ambiguous (no results returned because
                         * there were too many)
                         */
                        String noPathReason = "";

                        if (!shouldCalc) {
                            noPathReason = "Entry path calculation is disabled.";
                        } else if (!entriesNotEmpty) {
                            noPathReason = "Not available - too many results in archive.";
                        }
                        resultList.add(new ArchiveSearchResult(fileInfo, escapedExp, noPathReason, false));
                    }
                }
            } catch (RepositoryRuntimeException re) {
                JcrHelper.handleNotFoundException(re);
            }
        }
        return new ItemSearchResults<ArchiveSearchResult>(resultList, fullResults);
    }

    /**
     * Produces a list of content search entries based on a given excerpt
     *
     * @param excludeInnerClasses True if inner classes should be excluded from results
     * @param excerpt             Highlighted excerpt
     * @return List of results if found. Null if all results have been excluded
     */
    @SuppressWarnings({"unchecked"})
    private List<ArchiveSearchEntry> getContentSearchEntries(boolean excludeInnerClasses, String excerpt) {
        List<ArchiveSearchEntry> entriesToInclude = Lists.newArrayList();

        long highlightCount = 0;
        //Divide excerpt by fragments
        Element[] fragments = getFragmentElements(excerpt);
        for (Element fragment : fragments) {

            List<Element> highlights = fragment.getChildren(HIGHLIGHT_TAG);
            highlightCount += highlights.size();

            for (Element highlight : highlights) {
                String entryName;
                String completePath = highlight.getText();

                if ((completePath != null) && (!excludeInnerClasses || !completePath.contains("$"))) {
                    int lastSlash = completePath.lastIndexOf('/');
                    if (lastSlash == -1) {
                        entryName = completePath;
                    } else {
                        entryName = completePath.substring(lastSlash + 1);
                    }

                    ArchiveSearchEntry searchEntry = new ArchiveSearchEntry(entryName, completePath);
                    entriesToInclude.add(searchEntry);
                }
            }
        }

        /**
         * If no entries are chosen, but highlights were available, return null instead of list so we can determine if
         * results were all excluded, or simply couldn't be extracted
         */
        if ((entriesToInclude.size() == 0) && (highlightCount > 0)) {
            return null;
        }

        return entriesToInclude;
    }

    /**
     * Validates the length of the given query
     *
     * @param query Query to validate
     * @throws InvalidQueryRuntimeException If whitespace and wildcard-stripped query is less than 3 characters
     */
    private void validateQueryLength(String query) {
        String trimmedQuery = PathUtils.trimWhitespace(query);

        if (trimmedQuery.startsWith("*")) {
            trimmedQuery = trimmedQuery.substring(1);
        }
        if (trimmedQuery.endsWith("*")) {
            trimmedQuery = trimmedQuery.substring(0, trimmedQuery.length() - 1);
        }

        if (trimmedQuery.length() < ConstantValues.searchArchiveMinQueryLength.getLong()) {
            throw new InvalidQueryRuntimeException("Search term must be at least " +
                    ConstantValues.searchArchiveMinQueryLength.getString() + " characters long.");
        }
    }
}