package org.artifactory.search.build;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResults;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildBean;
import static org.artifactory.build.api.BuildBean.ARTIFACT;
import org.artifactory.build.api.BuildService;
import org.artifactory.jcr.JcrPath;
import static org.artifactory.jcr.JcrService.NODE_ARTIFACTORY_XML;
import org.artifactory.search.SearcherBase;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds the build search logic
 *
 * @author Noam Y. Tenne
 */
public class BuildSearcher extends SearcherBase {

    private BuildService buildService;

    /**
     * Default constructor
     */
    public BuildSearcher() {
        buildService = ContextHelper.get().beanForType(BuildService.class);
    }

    /**
     * Returns a list of build concentrated by name and latest date
     *
     * @return List of latest builds by name
     * @throws RepositoryException Any exception that might occur while executing the query
     */
    public List<Build> getLatestBuildsByName() throws Exception {

        Map<String, Build> map = Maps.newHashMap();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root").append(JcrPath.get().getBuildsJcrRootPath()).append("/*/*/element(*)");

        QueryResult queryResult = getJcrService().executeXpathQuery(queryBuilder.toString());

        RowIterator rows = queryResult.getRows();

        while (rows.hasNext()) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();

            String buildXml = getJcrService().getXml(path);
            Build build = buildService.getBuildFromXml(buildXml);

            String buildName = build.getName();

            if (!map.containsKey(buildName)) {
                map.put(buildName, build);
            } else {
                if (build.getNumber() > map.get(buildName).getNumber()) {
                    map.put(buildName, build);
                }
            }
        }

        return Lists.newArrayList(map.values());
    }

    /**
     * Locates builds with deployed artifacts that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of builds that deployed at least one artifact with the given checksum
     */
    public List<Build> findBuildsByArtifactChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(ARTIFACT, sha1, md5);
    }

    /**
     * Locates builds with dependencies that have the given checksum
     *
     * @param sha1 SHA1 checksum to search for. Can be blank.
     * @param md5  MD5 checksum to search for. Can be blank.
     * @return List of builds that depend on the artifact with the given checksum
     */
    public List<Build> findBuildsByDependencyChecksum(String sha1, String md5) throws RepositoryException {
        return findBuildsByItemChecksums(BuildBean.DEPENDENCY, sha1, md5);
    }

    /**
     * Locates builds that are named as the given name
     *
     * @param buildName Name of builds to locate
     * @return List of builds with the given name
     */
    public List<Build> searchBuildsByName(String buildName) throws RepositoryException {
        List<Build> results = Lists.newArrayList();

        if (StringUtils.isBlank(buildName)) {
            return results;
        }

        /**
         * Here it is only possible to perform a LIKE query to get the direct child down to a depth of one, so we can
         * only get down to the build number node, and from there we must iterate manually.
         * Currently when trying to perform a - path like '/x/%/%' and not path like '/x/%/%/%' - the complete list
         * Of children is returned. Should change in JCR 2.0
         * (http://mail-archives.apache.org/mod_mbox/jackrabbit-users/200803.mbox/%3C47DE81EE.6070809@gmx.net%3E)
         */

        StringBuilder queryBuilder = new StringBuilder();
        String escapedBuildName = Text.escapeIllegalJcrChars(buildName);
        queryBuilder.append("select * from nt:unstructured where jcr:path like '").
                append(JcrPath.get().getBuildsJcrRootPath()).append("/").append(escapedBuildName).
                append("/%' and not jcr:path like '").append(JcrPath.get().getBuildsJcrRootPath()).append("/").
                append(escapedBuildName).append("/%/%'");

        QueryResult queryResult = getJcrService().executeSqlQuery(queryBuilder.toString());

        NodeIterator buildNumberNodes = queryResult.getNodes();

        while (buildNumberNodes.hasNext()) {
            Node buildNumberNode = buildNumberNodes.nextNode();

            NodeIterator buildStartedNodes = buildNumberNode.getNodes();

            while (buildStartedNodes.hasNext()) {

                Node buildStartedNode = buildStartedNodes.nextNode();

                String buildXml = getJcrService().getXml(buildStartedNode.getPath());
                Build build = buildService.getBuildFromXml(buildXml);
                results.add(build);
            }
        }

        return results;
    }

    /**
     * Returns the latest build for the given name and number
     *
     * @param buildName   Name of build to locate
     * @param buildNumber Number of build to locate
     * @return Latest build if found. Null if not
     */
    public Build getLatestBuildByNameAndNumber(String buildName, long buildNumber) throws Exception {
        if (StringUtils.isBlank(buildName)) {
            return null;
        }

        StringBuilder queryBuilder = new StringBuilder();
        String escapedBuildName = Text.escapeIllegalJcrChars(buildName);
        queryBuilder.append("select * from nt:unstructured where jcr:path like '").
                append(JcrPath.get().getBuildsJcrRootPath()).append("/").append(escapedBuildName).append("/").
                append(buildNumber).append("/%' and not jcr:path like '").append(JcrPath.get().getBuildsJcrRootPath()).
                append("/").append(escapedBuildName).append("/").append(buildNumber).append("/%/%'");

        QueryResult queryResult = getJcrService().executeSqlQuery(queryBuilder.toString());

        RowIterator rows = queryResult.getRows();

        if (rows.hasNext()) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();

            String buildXml = getJcrService().getXml(path);
            return buildService.getBuildFromXml(buildXml);
        }

        return null;
    }

    /**
     * DO NOT USE - NOT IMPLEMENTED
     */
    @Override
    public SearchResults doSearch(SearchControls controls) throws RepositoryException {
        return null;
    }

    /**
     * Locates builds the produce or depened on an item with the given checksums and adds them to the given list
     *
     * @param itemType Item type. May be dependency or artifact
     * @param sha1     SHA1 checksum. May be blank
     * @param md5      MD5 checksum. May be blank
     * @return List of results
     */
    private List<Build> findBuildsByItemChecksums(String itemType, String sha1, String md5) throws RepositoryException {
        List<Build> results = Lists.newArrayList();

        if (StringUtils.isNotBlank(sha1)) {
            findBuildsByItemChecksum(itemType, "sha1", sha1, results);
        }

        if (results.isEmpty() && StringUtils.isNotBlank(md5)) {
            findBuildsByItemChecksum(itemType, "md5", md5, results);
        }

        return results;
    }

    /**
     * Locates builds the produce or depened on an item with the given checksum and adds them to the given list
     *
     * @param itemType      Item type. May be dependency or artifact
     * @param checksumType  Checksum type. May be SHA1 or MD5
     * @param checksumValue Item checksum value
     * @param results       List of results to append to
     */
    private void findBuildsByItemChecksum(String itemType, String checksumType, String checksumValue,
            List<Build> results) throws RepositoryException {
        String query = getChecksumQuery(itemType, checksumType, checksumValue);

        QueryResult queryResult = getJcrService().executeXpathQuery(query);

        RowIterator rows = queryResult.getRows();

        ArrayList<String> pathList = Lists.newArrayList();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();

            //Extract node path
            int artifactoryXmlStartIndex = path.indexOf("/" + NODE_ARTIFACTORY_XML);
            String buildNodePath = path.substring(0, artifactoryXmlStartIndex);
            if (!pathList.contains(buildNodePath)) {
                pathList.add(buildNodePath);
            }
        }

        for (String path : pathList) {
            String buildXml = getJcrService().getXml(path);
            Build build = buildService.getBuildFromXml(buildXml);
            results.add(build);
        }
    }

    /**
     * Returns the JCR XPath query for locating items with the given checksum
     *
     * @param itemType      Item type. May be dependency or artifact
     * @param checksumType  Checksum type. May be SHA1 or MD5
     * @param checksumValue Item checksum value
     * @return JCR Xpath query
     */
    private String getChecksumQuery(String itemType, String checksumType, String checksumValue) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root").append(JcrPath.get().getBuildsJcrRootPath()).append("//");
        addElementsToSb(queryBuilder, itemType, checksumType);
        queryBuilder.append("jcr:xmltext [@jcr:xmlcharacters = '").append(checksumValue).append("']");
        return queryBuilder.toString();
    }
}