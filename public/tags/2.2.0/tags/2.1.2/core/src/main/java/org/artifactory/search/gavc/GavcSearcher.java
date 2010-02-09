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

package org.artifactory.search.gavc;

import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.search.SearcherBase;
import org.springframework.util.StringUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the GAVC search logic
 *
 * @author Noam Tenne
 */
public class GavcSearcher extends SearcherBase<GavcSearchControls, GavcSearchResult> {

    @Override
    public SearchResults<GavcSearchResult> doSearch(GavcSearchControls controls) throws RepositoryException {

        //Validate and escape all input values
        String starWildcard = "*";
        String groupInput = validate(controls.getGroupId(), "");
        groupInput = escapeGroupPath(groupInput);
        String artifactId =
                validateAndEscape(controls.getArtifactId(), starWildcard, controls.isArtifactExactMatch(), true);
        String version = validateAndEscape(controls.getVersion(), starWildcard, controls.isVersionExactMatch(), true);
        String classifier = validateAndEscape(controls.getClassifier(), "%", true, false);

        //Build search path from inputted group
        StringBuilder pathBuilder = new StringBuilder();
        if (!controls.isGroupExactMatch() || groupInput.length() == 0) {
            pathBuilder.append("/");
        }
        if (groupInput.length() > 0) {
            String[] groups = groupInput.split("/");
            for (String group : groups) {
                String groupId = escape(group, controls.isGroupExactMatch(), true);
                pathBuilder.append(groupId).append("/");
            }
            if (!controls.isGroupExactMatch()) {
                pathBuilder.append("/");
            }
        }

        pathBuilder.append(artifactId).append("/");

        /**
         * If a version is given, it needs special handeling. A node path cannot begin with a number (can be escaped
         * With the class - ISO9075. This method is not used, because after encoding the digits, we cannot use wildcards
         * On it. Meanwhile using the jcr:like function.
         */
        pathBuilder.append(version).append("/");

        pathBuilder.append("element(*, ").append(JcrFile.NT_ARTIFACTORY_FILE).append(") ");
        pathBuilder.append("[jcr:like(@").append(JcrHelper.PROP_ARTIFACTORY_NAME).append(",'")
                .append("%-").append(classifier).append(".%").append("')] ");
        String path = pathBuilder.toString();
        String queryStr = "/jcr:root" + JcrPath.get().getRepoJcrRootPath() + "/*/" + path + "order by @" +
                JcrHelper.PROP_ARTIFACTORY_NAME + " ascending";

        QueryResult queryResult = getJcrService().executeXpathQuery(queryStr);
        List<GavcSearchResult> results = new ArrayList<GavcSearchResult>();
        NodeIterator nodes = queryResult.getNodes();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (nodes.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            Node artifactNode = nodes.nextNode();
            RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (localRepo == null) {
                // Some left over in JCR of non configured repo
                continue;
            }

            if (NamingUtils.isChecksum(repoPath.getPath())) {
                // don't show checksum files
                continue;
            }

            FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
            ArtifactResource artifact = new ArtifactResource(fileInfo.getRepoPath());
            boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
            MavenArtifactInfo mavenInfo = artifact.getMavenInfo();
            if (canRead && mavenInfo.isValid()) {
                GavcSearchResult result = new GavcSearchResult(fileInfo, mavenInfo);
                results.add(result);
            }
        }

        return new SearchResults<GavcSearchResult>(results, nodes.getSize());
    }

    /**
     * Swaps all backward-slashes ('\') to forward ones ('/'). Removes leading and trailing slashes (if any), Replaces
     * all periods ('.') to forward slashes.
     *
     * @param groupInput The inputted group path
     * @return String - Group path after escape
     */
    private String escapeGroupPath(String groupInput) {
        if (groupInput != null) {
            groupInput = groupInput.replace('\\', '/');
            groupInput = StringUtils.trimLeadingCharacter(groupInput, '/');
            groupInput = StringUtils.trimTrailingCharacter(groupInput, '/');
            groupInput = groupInput.replace('.', '/');
        }
        return groupInput;
    }

    /**
     * If the given input will be null or empty, the method will return the given wildcard to replace the expression. If
     * the given input is valid, it will be escaped (jcr:like compatible string) and surrounded with a jcr:like
     * function
     *
     * @param userInput  Search form input
     * @param wildcard   If the expression is null or empty, the method will return it to act as a wildcard in the
     *                   query
     * @param exactMatch Search without wildcards surrounding input
     * @param wrap       Wrap input with jcr:like expression
     * @return String - Validated and escaped input
     */
    private String validateAndEscape(String userInput, String wildcard, boolean exactMatch, boolean wrap) {
        userInput = validate(userInput, wildcard);
        if (userInput.equals(wildcard)) {
            return wildcard;
        }

        userInput = escape(userInput, exactMatch, wrap);
        return userInput;
    }

    /**
     * Validates the given input. When null or empty, the method will return the given wildcard. When valid, it will
     * return the input
     *
     * @param userInput Search form input
     * @param wildcard  If the expression is null or empty, the method will return it to act as a wildcard in the query
     * @return String - Validated input
     */
    private String validate(String userInput, String wildcard) {
        if ((userInput == null) || (!StringUtils.hasText(userInput))) {
            return wildcard;
        }

        return userInput;
    }

    /**
     * Escapes the input in order to make it compatible
     *
     * @param userInput  Search form input
     * @param exactMatch Search without wildcards surrounding input
     * @param wrap       Wrap input with jcr:like expression
     * @return String - Escaped input.
     */
    private String escape(String userInput, boolean exactMatch, boolean wrap) {
        String input = escapeToJcrLikeString(userInput);
        if (!exactMatch) {
            if (!input.startsWith("%")) {
                input = "%" + input;
            }
            if (!input.endsWith("%")) {
                input += "%";
            }
        }
        if (wrap) {
            input = wrap(input);
        }

        return input;
    }

    /**
     * Wraps the input in a jcr:like function.
     *
     * @param userInput Search form input
     * @return String - Wrapped input.
     */
    private String wrap(String userInput) {
        return "element(*)[jcr:like(@" + JcrHelper.PROP_ARTIFACTORY_NAME + ",'" + userInput + "')]";
    }
}
