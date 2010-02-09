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
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.log.LoggerFactory;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.search.SearcherBase;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
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
        String escapedExp = Text.escapeIllegalXpathSearchChars(controls.getQuery());

        /**
         * We cross queries between the property level and the node level so we can utilize the custom analyzer, and get
         * highlighted excerpts, but the node level search sees the archive entries with their full path (unlike the
         * custom analyzer that strips paths), so we put a wildcard in-front of the search term (if doesn't already
         * exist) to cover any path that may precede the entry name
         */

        ////*[jcr:contains(@artifactory:archiveEntry,'bufferrow*') and jcr:contains(.,'*/bufferrow*')]
        StringBuilder queryBuilder = getPathQueryBuilder(controls);
        queryBuilder.append("/*[jcr:contains(@").append(JcrTypes.PROP_ARTIFACTORY_ARCHIVE_ENTRY).append(", '").
                append(escapedExp).append("')]");
        String queryStr = queryBuilder.toString();
        log.debug("Executing archive search query: {}", queryStr);

        JcrQuerySpec spec = JcrQuerySpec.xpath(queryStr);
        if (!controls.isLimitSearchResults()) {
            spec.noLimit();
        }
        QueryResult queryResult = getJcrService().executeQuery(spec);
        List<ArchiveSearchResult> resultList = Lists.newArrayList();
        RowIterator rows = queryResult.getRows();
        long fullResults = 0;

        while (rows.hasNext()) {
            Row row = rows.nextRow();
            try {
                String excerpt =
                        row.getValue("rep:excerpt(" + JcrTypes.PROP_ARTIFACTORY_ARCHIVE_ENTRY + ")").getString();
                log.debug("Archive search excerpt result: {}", excerpt);
                List<ArchiveSearchEntry> entriesToInclude = getContentSearchEntries(excerpt);
                int entriesSize = entriesToInclude.size();
                fullResults += (entriesSize == 0) ? 1 : entriesSize;

                //If the search results are limited, stop when reached more than max results + 1
                if (!controls.isLimitSearchResults() || (resultList.size() < getMaxResults())) {
                    String artifactPath = row.getValue(JcrConstants.JCR_PATH).getString();
                    RepoPath repoPath = JcrPath.get().getRepoPath(artifactPath);
                    if ((repoPath == null) || (!controls.isSpecificRepoSearch() && !isResultRepoPathValid(repoPath))) {
                        continue;
                    }

                    FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
                    boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
                    MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(fileInfo.getRepoPath());
                    if (canRead && mavenInfo.isValid()) {

                        boolean shouldCalc = controls.shouldCalcEntries();
                        boolean entriesNotEmpty = (entriesSize != 0);

                        //If we need only the files (i.e. when saving), skip entries calculation to speed up things
                        if (shouldCalc && entriesNotEmpty) {
                            for (ArchiveSearchEntry entry : entriesToInclude) {
                                if (!entry.getEntryPath().equals(fileInfo.getName())) {
                                    String entryName = entry.getEntryName();
                                    if (StringUtils.isEmpty(entryName)) {
                                        entryName = escapedExp;
                                    }
                                    ArchiveSearchResult result =
                                            new ArchiveSearchResult(fileInfo, mavenInfo, entryName,
                                                    entry.getEntryPath());
                                    resultList.add(result);
                                }
                            }
                        } else {
                            String noPathReason = "";

                            if (!shouldCalc) {
                                noPathReason = "Entry path calculation is disabled.";
                            } else if (!entriesNotEmpty) {
                                noPathReason = "Not available - too many results in archive.";
                            }
                            ArchiveSearchResult result = new ArchiveSearchResult(fileInfo, mavenInfo, escapedExp,
                                    noPathReason);
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
     * @param excerpt Highlighted excerpt
     * @return ArrayList<ContentSearchEntry> - List of ContentSearchEntry objects
     */
    @SuppressWarnings({"unchecked"})
    private List<ArchiveSearchEntry> getContentSearchEntries(String excerpt) {
        List<ArchiveSearchEntry> entriesToInclude = Lists.newArrayList();

        //Divide excerpt by fragments
        Element[] fragments = getFragmentElements(excerpt);
        for (Element fragment : fragments) {

            List<Element> highlights = fragment.getChildren(HIGHLIGHT_TAG);

            for (Element highlight : highlights) {
                String entryName = "";
                String completePath = highlight.getText();

                int lastSlash = completePath.lastIndexOf('/');
                if (lastSlash == -1) {
                    entryName = completePath;
                    completePath = "/";
                } else {
                    entryName = completePath.substring(lastSlash + 1);
                }

                ArchiveSearchEntry searchEntry = new ArchiveSearchEntry(entryName, completePath);
                entriesToInclude.add(searchEntry);
            }
        }

        return entriesToInclude;
    }
}