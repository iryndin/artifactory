/*
 * This file is part of Artifactory.
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

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.log.LoggerFactory;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;
import org.jdom.Content;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Noam Tenne
 */
public class ArchiveSearcher extends SearcherBase<ArchiveSearchControls, ArchiveSearchResult> {

    private static final Logger log = LoggerFactory.getLogger(ArchiveSearcher.class);

    private static final String CLASS_SUFFIX = ".class";
    private static final String HIGHLIGHT_TAG = "highlight";

    @Override
    public SearchResults<ArchiveSearchResult> doSearch(ArchiveSearchControls controls) throws RepositoryException {
        boolean exactMatch = controls.isExactMatch();
        boolean searchAllTypes = controls.isSearchAllTypes();

        String exp = buildSearchExpression(controls.getQuery(), exactMatch, searchAllTypes);

        StringBuilder queryBuilder = getPathQueryBuilder(controls);
        queryBuilder.append("/element(*,").append(JcrFile.NT_ARTIFACTORY_FILE).append(") [jcr:contains(.,");
        queryBuilder.append(exp).append(")]/@").append(ArchiveIndexer.PROP_ARTIFACTORY_ARCHIVE_ENTRY)
                .append("");
        String queryStr = queryBuilder.toString();
        log.debug("Executing archive search query: {}", queryStr);
        QueryResult queryResult = getJcrService().executeXpathQuery(queryStr);
        List<ArchiveSearchResult> resultList = new ArrayList<ArchiveSearchResult>();
        RowIterator rows = queryResult.getRows();
        long fullResults = 0;

        while (rows.hasNext()) {
            Row row = rows.nextRow();
            String excerpt = row.getValue("rep:excerpt(.)").getString();
            log.debug("Archive search excerpt result: {}", excerpt);
            List<ArchiveSearchEntry> entriesToInclude = getContentSearchEntries(excerpt);
            fullResults += entriesToInclude.size();

            //If the search results are limited, stop when reached more than max results + 1
            if (!controls.isLimitSearchResults() || (resultList.size() < getMaxResults())) {
                String artifactPath = row.getValue(JcrConstants.JCR_PATH).getString();
                RepoPath repoPath = JcrPath.get().getRepoPath(artifactPath);
                if (!isResultRepoPathValid(repoPath)) {
                    continue;
                }

                FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
                boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
                MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(fileInfo.getRepoPath());
                if (canRead && mavenInfo.isValid()) {
                    if (controls.shouldCalcEntries()) {
                        for (ArchiveSearchEntry entry : entriesToInclude) {
                            if (!entry.getEntryPath().equals(fileInfo.getName())) {
                                String entryName = entry.getEntryName();
                                if (StringUtils.isEmpty(entryName)) {
                                    entryName = exp;
                                }
                                ArchiveSearchResult result =
                                        new ArchiveSearchResult(fileInfo, mavenInfo, entryName, entry.getEntryPath());
                                resultList.add(result);
                            }
                        }
                    } else {
                        //If we need only the files (i.e. when saving), skip entries calculation to speed up things
                        ArchiveSearchResult result = new ArchiveSearchResult(fileInfo, mavenInfo, null, null);
                        resultList.add(result);
                    }
                }
            }
        }
        return new SearchResults<ArchiveSearchResult>(resultList, fullResults);
    }

    /**
     * Builds the search expression according to the user's search method selection
     *
     * @param search         User search input
     * @param exactMatch     Is an exact match requested
     * @param searchAllTypes User search method selection
     * @return String - search expression
     */
    private String buildSearchExpression(String search, boolean exactMatch, boolean searchAllTypes) {
        String exp;

        //If the search is specified for classes only
        if (searchAllTypes) {
            //Try to detect given extension
            int extensionIndex = search.lastIndexOf('.');
            if (extensionIndex != -1) {
                String pathNoExtension = search.substring(0, extensionIndex);
                String extensionToAdd = "." + PathUtils.getExtension(search);
                exp = formatContentSearchPath(pathNoExtension, extensionToAdd, exactMatch);
            } else {
                exp = formatContentSearchPath(search, null, exactMatch);
            }
        } else {
            //Make sure the class suffix exists
            if (search.endsWith(CLASS_SUFFIX)) {
                search = StringUtils.remove(search, CLASS_SUFFIX);
            }
            exp = formatContentSearchPath(search, CLASS_SUFFIX, exactMatch);
        }
        return exp;
    }

    private String formatContentSearchPath(String search, String extension, boolean exactMatch) {
        String extensionToAdd = "";
        //Set the given extension only if valid
        if (StringUtils.isNotBlank(extension)) {
            extensionToAdd = extension;
        }
        String exp = escapeToJcrContainsString(search);
        /**
         * Replace all dots (representing package seperation) to slashes ('/') and all dollar signs (representing
         * Inner class) to dots, since this is the way the paths are indexed when deployed.
         */
        exp = exp.replace('.', '/').replace('$', '.');

        StringBuilder builder = new StringBuilder();

        builder.append("'");

        //If there an exact match requested
        if (exactMatch) {
            /**
             * Add a forward slash to the begining of the expression to make sure it will return and exact match of the
             * class name
             */
            builder.append("/");
        } else {
            //Add a wildcard to the begining
            builder.append("*");
        }

        builder.append(exp);

        //If there is no exact match requested
        if (!exactMatch) {
            //Add a wildcard to the end
            builder.append("*");
        }
        builder.append(extensionToAdd).append("'");
        return builder.toString();
    }

    /**
     * Produces a list of content search entries based on a given excerpt
     *
     * @param excerpt Highlighted excerpt
     * @return ArrayList<ContentSearchEntry> - List of ContentSearchEntry objects
     */
    @SuppressWarnings({"unchecked"})
    private List<ArchiveSearchEntry> getContentSearchEntries(String excerpt) {
        List<ArchiveSearchEntry> entriesToInclude = new ArrayList<ArchiveSearchEntry>();

        //Divide excerpt by fragments
        Element[] fragments = getFragmentElements(excerpt);
        for (Element fragment : fragments) {
            List highlightList = fragment.getChildren(HIGHLIGHT_TAG);
            if (highlightList.size() > 0) {
                List<Content> children = fragment.getContent();
                for (Content fragmentChild : children) {
                    StringBuilder path = new StringBuilder();

                    //Make sure child is of Element kind, and that is of a </highlight> type
                    if (fragmentChild instanceof Element) {
                        String childName = ((Element) fragmentChild).getName();
                        if (HIGHLIGHT_TAG.equals(childName)) {
                            int childIndex = children.indexOf(fragmentChild);

                            //Collect path before highlight
                            getPathBeforeHighlight(children, path, childIndex);
                            path.append(fragmentChild.getValue());

                            //Collect path after highlight
                            getPathAfterHighlight(children, path, childIndex);
                            ArchiveSearchEntry searchEntry =
                                    new ArchiveSearchEntry(fragmentChild.getValue(), path.toString());
                            entriesToInclude.add(searchEntry);
                        }
                    }
                }
            } else {
                String fragmentValue = fragment.getValue();
                String[] paths = fragmentValue.split(" ");
                for (String path : paths) {
                    ArchiveSearchEntry searchEntry = new ArchiveSearchEntry(path);
                    entriesToInclude.add(searchEntry);
                }
            }
        }

        return entriesToInclude;
    }
}