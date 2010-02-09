package org.artifactory.build;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.build.api.Artifact;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildFileBean;
import org.artifactory.build.api.Dependency;
import org.artifactory.build.api.Module;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.log.LoggerFactory;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Build service main implementation
 *
 * @author Noam Y. Tenne
 */
@Service
@Reloadable(beanClass = InternalBuildService.class, initAfter = JcrService.class)
public class BuildServiceImpl implements InternalBuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalSearchService searchService;

    public void init() {
        //Create initial builds folder
        jcrService.getOrCreateUnstructuredNode(getJcrPath().getBuildsJcrRootPath());
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        //Nothing to reload
    }

    public void destroy() {
        //Nothing to destroy
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void addBuild(Build build) {
        InternalBuildService internalBuildService = InternalContextHelper.get().beanForType(InternalBuildService.class);
        internalBuildService.transactionalAddBuild(build);
    }

    public void transactionalAddBuild(Build build) {
        populateMissingChecksums(build);

        String buildName = build.getName();
        String esacpedBuildName = Text.escapeIllegalJcrChars(buildName);
        long buildNumber = build.getNumber();
        String escapedStarted = Text.escapeIllegalJcrChars(build.getStarted());

        Node buildsNode = jcrService.getOrCreateUnstructuredNode(getJcrPath().getBuildsJcrRootPath());
        Node buildNameNode = jcrService.getOrCreateUnstructuredNode(buildsNode, esacpedBuildName);
        Node buildNumberNode = jcrService.getOrCreateUnstructuredNode(buildNameNode, Long.toString(buildNumber));

        String buildXml = getXmlFromBuild(build);

        jcrService.setXml(buildNumberNode, escapedStarted, buildXml, true, SecurityService.USER_SYSTEM);

        log.debug("Added info for build '{}' #{}", buildName, buildNumber);
    }

    public String getXmlFromBuild(Build build) {
        return getXStream().toXML(build);
    }

    public Build getBuildFromXml(String buildXml) {
        return (Build) getXStream().fromXML(buildXml);
    }

    public void deleteBuild(Build build) {
        String escapedName = Text.escapeIllegalJcrChars(build.getName());
        String escapedStarted = Text.escapeIllegalJcrChars(build.getStarted());

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(JcrPath.get().getBuildsJcrRootPath()).append("/").append(escapedName).append("/").
                append(build.getNumber()).append("/").append(escapedStarted);

        jcrService.delete(pathBuilder.toString());
    }

    public FileInfo getBestMatchingResult(List<RepoPath> searchResults, Map<RepoPath, Properties> resultProperties,
            String buildName, long buildNumber) {

        if (resultProperties.isEmpty()) {
            return getLatestItem(searchResults);
        } else {
            return matchResultBuildNameAndNumber(searchResults, resultProperties, buildName, buildNumber);
        }
    }

    /**
     * Locates and fills in missing checksums of the module artifacts and dependencies
     *
     * @param build Build to populate
     */
    private void populateMissingChecksums(Build build) {

        for (Module module : build.getModules()) {

            for (Artifact artifact : module.getArtifacts()) {
                populateBeanChecksum(artifact);
            }

            for (Dependency dependency : module.getDependencies()) {
                populateBeanChecksum(dependency);
            }
        }
    }

    /**
     * Locates and fills in missing checksums of the given build file bean
     *
     * @param bean Bean to fill in missing checksums for
     */
    private void populateBeanChecksum(BuildFileBean bean) {
        String sha1 = bean.getSha1();
        String md5 = bean.getMd5();

        List<RepoPath> artifactList = searchService.searchArtifactsByChecksum(sha1, md5);

        if (!artifactList.isEmpty()) {
            RepoPath repoPath = artifactList.get(0);
            FileInfo fileInfo = new FileInfoProxy(repoPath);

            if (StringUtils.isBlank(sha1)) {
                bean.setSha1(fileInfo.getSha1());
            } else if (StringUtils.isBlank(md5)) {
                bean.setMd5(fileInfo.getMd5());
            }
        }
    }

    /**
     * Returns the best matching file info object by build name and number
     *
     * @param searchResults    File bean search results
     * @param resultProperties Search result property map
     * @param buildName        Build name to search for
     * @param buildNumber      Build number to search for
     * @return The file info of a result that best matches the given build name and number
     */
    private FileInfo matchResultBuildNameAndNumber(List<RepoPath> searchResults,
            Map<RepoPath, Properties> resultProperties, String buildName, long buildNumber) {
        Map<RepoPath, Properties> matchingBuildNames = Maps.newHashMap();

        for (RepoPath repoPath : resultProperties.keySet()) {
            Properties properties = resultProperties.get(repoPath);
            Set<String> buildNames = properties.get("build.name");
            if (buildNames.contains(buildName)) {
                matchingBuildNames.put(repoPath, properties);
            }
        }

        if (matchingBuildNames.isEmpty()) {
            return getLatestItem(searchResults);
        } else {
            return matchResultBuildNumber(resultProperties, matchingBuildNames, buildNumber);
        }
    }

    /**
     * Returns the best matching file info object by build number
     *
     * @param resultProperties Search result property map
     * @param matchingPaths    File info paths that match by build name
     * @param buildNumber      Build number to search for
     * @return The file info of a result that best matches the given build number
     */
    private FileInfo matchResultBuildNumber(Map<RepoPath, Properties> resultProperties,
            Map<RepoPath, Properties> matchingPaths, long buildNumber) {
        RepoPath selectedPath = matchingPaths.keySet().iterator().next();
        long bestMatch = 0;

        for (RepoPath repoPath : matchingPaths.keySet()) {
            Properties properties = resultProperties.get(repoPath);
            Set<Long> buildNumbers = convertBuildNumberProperties(properties.get("build.number"));
            if (buildNumbers.contains(buildNumber)) {
                selectedPath = repoPath;
                break;
            } else {
                for (Long number : buildNumbers) {
                    if ((number > bestMatch) && (number <= buildNumber)) {
                        selectedPath = repoPath;
                        bestMatch = number;
                    }
                }
            }
        }

        return new FileInfoProxy(selectedPath);
    }

    /**
     * Returns the file info object of the result watch was last modified
     *
     * @param searchResults Search results to search within
     * @return Latest modified search result file info. Null if no results were given
     */
    private FileInfo getLatestItem(List<RepoPath> searchResults) {
        FileInfo latestItem = null;

        for (RepoPath result : searchResults) {
            FileInfo fileInfo = new FileInfoProxy(result);
            if ((latestItem == null) || (latestItem.getLastModified() < fileInfo.getLastModified())) {
                latestItem = fileInfo;
            }
        }
        return latestItem;
    }

    /**
     * Converts a set of build number string properties to a set of longs
     *
     * @param buildNumberStrings Build number string set
     * @return Build number long set
     */
    private Set<Long> convertBuildNumberProperties(Set<String> buildNumberStrings) {
        Set<Long> buildNumbers = Sets.newHashSet();
        for (String numberString : buildNumberStrings) {
            try {
                long buildNumber = Long.parseLong(numberString);
                buildNumbers.add(buildNumber);
            } catch (NumberFormatException e) {
                log.error("Found non-long build number '{}'", numberString);
            }
        }

        return buildNumbers;
    }

    /**
     * Returns the JCR path helper object
     *
     * @return JcrPath
     */
    private JcrPath getJcrPath() {
        return JcrPath.get();
    }

    /**
     * Returns an XStream instance ready with build object configured
     *
     * @return XStream instance
     */
    private XStream getXStream() {
        return XStreamFactory.create(Build.class);
    }
}