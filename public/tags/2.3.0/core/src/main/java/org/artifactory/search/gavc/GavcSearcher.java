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

package org.artifactory.search.gavc;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.search.SearcherBase;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.List;

/**
 * Holds the GAVC search logic
 *
 * @author Noam Tenne
 */
public class GavcSearcher extends SearcherBase<GavcSearchControls, GavcSearchResult> {

    @Override
    public SearchResults<GavcSearchResult> doSearch(GavcSearchControls controls) throws RepositoryException {
        StringBuilder queryBuilder = getPathQueryBuilder(controls);

        //Validate and escape all input values
        String groupInput = escapeGroupPath(controls.getGroupId());
        boolean groupContainsWildCard = inputContainsWildCard(groupInput);

        String artifactId = validateAndEscape(controls.getArtifactId(), true);
        String version = validateAndEscape(controls.getVersion(), true);
        String classifier = validateAndEscape(controls.getClassifier(), false);

        //Build search path from inputted group
        if (!controls.isSpecificRepoSearch() || groupContainsWildCard || groupInput.length() == 0) {
            queryBuilder.append("/");
        }
        if (groupInput.length() > 0) {
            String[] groups = groupInput.split("/");
            for (String group : groups) {
                String groupId = escape(group, true);
                queryBuilder.append(groupId).append("/");
            }
            if (groupContainsWildCard) {
                queryBuilder.append("/");
            }
        }

        if (StringUtils.isNotBlank(artifactId)) {
            queryBuilder.append(artifactId).append("/");
        } else if (!queryBuilder.toString().endsWith("//")) {
            queryBuilder.append("/");
        }

        /**
         * If a version is given, it needs special handling. A node path cannot begin with a number (can be escaped
         * With the class - ISO9075. This method is not used, because after encoding the digits, we cannot use wildcards
         * On it. 
         */
        if (StringUtils.isNotBlank(version)) {
            queryBuilder.append(version);
        }

        if (!queryBuilder.toString().endsWith("//")) {
            queryBuilder.append("/");
        }

        queryBuilder.append("element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).append(")");
        if (StringUtils.isNotBlank(classifier)) {
            queryBuilder.append(" [jcr:contains(@").append(JcrTypes.PROP_ARTIFACTORY_NAME).append(", '")
                    .append("*-").append(classifier).append(".*").append("')] ");
        }

        QueryResult queryResult = performQuery(controls.isLimitSearchResults(), queryBuilder.toString());
        List<GavcSearchResult> results = Lists.newArrayList();
        NodeIterator nodes = queryResult.getNodes();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (nodes.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            Node artifactNode = nodes.nextNode();
            RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
            if ((repoPath == null) || !isResultRepoPathValid(repoPath)) {
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
        if (StringUtils.isBlank(groupInput)) {
            groupInput = "";
        }
        groupInput = groupInput.replace('\\', '/');
        groupInput = org.springframework.util.StringUtils.trimLeadingCharacter(groupInput, '/');
        groupInput = org.springframework.util.StringUtils.trimTrailingCharacter(groupInput, '/');
        groupInput = groupInput.replace('.', '/');
        return groupInput;
    }

    /**
     * If the given input will be null or empty, the method will return the star wildcard to replace the expression. If
     * the given input is valid, it will be escaped (jcr:contains compatible string) and surrounded with a jcr:contains
     * function
     *
     * @param userInput Search form input
     * @param wrap      Wrap input with jcr:like expression
     * @return String - Validated and escaped input
     */
    private String validateAndEscape(String userInput, boolean wrap) {
        if (StringUtils.isBlank(userInput)) {
            return "";
        }

        userInput = escape(userInput, wrap);
        return userInput;
    }

    /**
     * Escapes the input in order to make it compatible
     *
     * @param userInput Search form input
     * @param wrap      Wrap input with jcr:contains expression
     * @return String - Escaped input.
     */
    private String escape(String userInput, boolean wrap) {
        String input = Text.escapeIllegalXpathSearchChars(userInput);

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
        if (inputContainsWildCard(userInput)) {
            return ". [jcr:contains(@" + JcrTypes.PROP_ARTIFACTORY_NAME + ", '" + userInput + "')]";
        } else {
            return ". [@" + JcrTypes.PROP_ARTIFACTORY_NAME + " = '" + userInput + "']";
        }
    }
}