/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.List;

/**
 * @author Noam Tenne
 */
public class ArchiveSearcher extends SearcherBase<ArchiveSearchControls, ArchiveSearchResult> {

    private static final Logger log = LoggerFactory.getLogger(ArchiveSearcher.class);

    private static final String HIGHLIGHT_TAG = "highlight";

    @Override
    public SearchResults<ArchiveSearchResult> doSearch(ArchiveSearchControls controls) throws RepositoryException {

        String query = controls.getQuery();
        validateQueryLength(query);

        String escapedExp = Text.escapeIllegalXpathSearchChars(query);

        /**
         * We cross queries between the property level and the node level so we can utilize the custom analyzer, and get
         * highlighted excerpts, but the node level search sees the archive entries with their full path (unlike the
         * custom analyzer that strips paths), so we put a wildcard in-front of the search term (if doesn't already
         * exist) to cover any path that may precede the entry name
         */

        ////*[jcr:contains(@artifactory:archiveEntry,'bufferrow*') and jcr:contains(.,'*/bufferrow*')]
        StringBuilder queryBuilder = getPathQueryBuilder(controls);
        queryBuilder.append("/element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).append(") [jcr:contains(@").
                append(JcrTypes.PROP_ARTIFACTORY_ARCHIVE_ENTRY).append(", '").append(escapedExp).append("')]");
        String queryStr = queryBuilder.toString();
        log.debug("Executing archive search query: {}", queryStr);

        QueryResult queryResult = performQuery(controls.isLimitSearchResults(), queryStr);
        List<ArchiveSearchResult> resultList = Lists.newArrayList();
        RowIterator rows = queryResult.getRows();
        long fullResults = 0;

        while (rows.hasNext()) {
            Row row = rows.nextRow();
            try {
                String excerpt =
                        row.getValue("rep:excerpt(" + JcrTypes.PROP_ARTIFACTORY_ARCHIVE_ENTRY + ")").getString();
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
                    String artifactPath = row.getValue(JcrConstants.JCR_PATH).getString();
                    RepoPath repoPath = JcrPath.get().getRepoPath(artifactPath);
                    if ((repoPath == null) || !isResultRepoPathValid(repoPath)) {
                        continue;
                    }

                    FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
                    boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
                    if (canRead) {

                        boolean shouldCalc = controls.shouldCalcEntries();
                        boolean entriesNotEmpty = (entriesSize != 0);

                        //If we need only the files (i.e. when saving), skip entries calculation to speed up things
                        if (shouldCalc && entriesNotEmpty) {
                            for (ArchiveSearchEntry entry : entriesToInclude) {
                                String entryName = entry.getEntryName();
                                if (StringUtils.isEmpty(entryName)) {
                                    entryName = escapedExp;
                                }
                                ArchiveSearchResult result =
                                        new ArchiveSearchResult(fileInfo, entryName, entry.getEntryPath());
                                resultList.add(result);
                            }
                        } else {
                            String noPathReason = "";

                            if (!shouldCalc) {
                                noPathReason = "Entry path calculation is disabled.";
                            } else if (!entriesNotEmpty) {
                                noPathReason = "Not available - too many results in archive.";
                            }
                            ArchiveSearchResult result = new ArchiveSearchResult(fileInfo, escapedExp, noPathReason);
                            resultList.add(result);
                        }
                    }
                }
            } catch (RepositoryException re) {
                handleNotFoundException(re);
            }
        }
        return new SearchResults<ArchiveSearchResult>(resultList, fullResults);
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
     * @throws InvalidQueryException If whitespace and wildcard-stripped query is less than 3 characters
     */
    private void validateQueryLength(String query) throws InvalidQueryException {
        String trimmedQuery = PathUtils.trimWhitespace(query);

        if (trimmedQuery.startsWith("*")) {
            trimmedQuery = trimmedQuery.substring(1);
        }
        if (trimmedQuery.endsWith("*")) {
            trimmedQuery = trimmedQuery.substring(0, trimmedQuery.length() - 1);
        }

        if (trimmedQuery.length() < ConstantValues.searchArchiveMinQueryLength.getLong()) {
            throw new InvalidQueryException("Search term must be at least " +
                    ConstantValues.searchArchiveMinQueryLength.getString() + " characters long.");
        }
    }
}