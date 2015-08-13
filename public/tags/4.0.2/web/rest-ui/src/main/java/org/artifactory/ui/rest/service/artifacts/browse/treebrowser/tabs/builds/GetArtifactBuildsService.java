package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tabs.builds;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.build.ArtifactBuildAddon;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SearchService;
import org.artifactory.build.ArtifactoryBuildArtifact;
import org.artifactory.build.BuildRun;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.builds.BuildsArtifactInfo;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.builds.ProduceBy;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.builds.UsedBy;
import org.artifactory.util.CollectionUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetArtifactBuildsService implements RestService {
    @Autowired
    RepositoryService repositoryService;
    @Autowired
    protected SearchService searchService;
    @Autowired
    protected BuildService buildService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String path = request.getQueryParamByKey("path");
        String repoKey = request.getQueryParamByKey("repoKey");
        RepoPath repoPath = InternalRepoPathFactory.create(repoKey, path);
        // get checksum info
        FileInfo buildFileInfo = getFileInfo(repoPath);
        String sha1 = buildFileInfo.getSha1();
        String md5 = buildFileInfo.getMd5();
        // fetch produce by data
        List<ProduceBy> produceByList = fetchProduceByData(sha1, md5);
        // fetch used by data
        List<UsedBy> usedByList = fetchUsedByData(sha1, md5);
        // update response data
        updateResponseData(response, produceByList, usedByList);
    }
    /**
     * update response with artifact build info
     *
     * @param artifactoryResponse - encapsulate data require for response
     * @param produceByList       - produce by data list
     * @param usedByList          - used by data list
     */
    private void updateResponseData(RestResponse artifactoryResponse, List<ProduceBy> produceByList,
            List<UsedBy> usedByList) {
        BuildsArtifactInfo buildsArtifactInfo = new BuildsArtifactInfo(produceByList, usedByList);
        artifactoryResponse.iModel(buildsArtifactInfo);
    }

    /**
     * get File info by repo pat
     *
     * @param repoPath - repo path
     * @return file info instance
     */
    private FileInfo getFileInfo(RepoPath repoPath) {
        ItemInfo fileInfo = repositoryService.getItemInfo(repoPath);
        return (FileInfo) fileInfo;
    }

    /**
     * fetch used by data
     *
     * @param sha1- sha1 checksum
     * @param md5   - md5 checksum
     * @return used by list
     */
    private List<UsedBy> fetchUsedByData(String sha1, String md5) {
        List<UsedBy> usedByList = new ArrayList<>();
        List<BuildRun> dependencyBuilds = getDependencyBuilds(sha1, md5);
        dependencyBuilds.forEach(dependencyBuild -> addBuildUsedBy(dependencyBuild, sha1, md5, usedByList));
        return usedByList;
    }

    /**
     * fetch produce by data
     *
     * @param sha1- sha1 checksum
     * @param md5   - md5 checksum
     * @return produce by list
     */
    private List<ProduceBy> fetchProduceByData(String sha1, String md5) {
        List<ProduceBy> produceByList = new ArrayList<>();
        List<BuildRun> artifactBuilds = getArtifactBuilds(sha1, md5);
        artifactBuilds.forEach(artifactBuild -> getArtifactProduceBy(artifactBuild, sha1, md5, produceByList));
        return produceByList;
    }

    private List<BuildRun> getArtifactBuilds(String sha1, String md5) {
        return Lists.newArrayList(searchService.findBuildsByArtifactChecksum(sha1, md5));
    }

    private List<BuildRun> getDependencyBuilds(String sha1, String md5) {
        return Lists.newArrayList(searchService.findBuildsByDependencyChecksum(sha1, md5));
    }

 /*   *//**
     * get Artifact build  Produce by data list
     *
     * @param run            - run build details
     * @param sha1           - sha1 checksum
     * @param md5            - md5 checksum
     * @param producedByList - produce by list
     * @return - list of produce by instances
     *//*
    protected List<ProduceBy> getArtifactProduceBy(BuildRun run, String sha1, String md5,
            List<ProduceBy> producedByList) {
        ArtifactBuildAddon buildAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(
                ArtifactBuildAddon.class);
        Build build = buildService.getBuild(run);
  *//*      List<Module> modules = build.getModules();
        if (modules != null) {
            modules.forEach(module ->
                    addBuildProduceBy(run, sha1, md5, buildAddon, producedByList, build, module));
        }*//*
        List<Module> modules = build.getModules();
        modules.forEach(module -> producedByList.add(new ProduceBy(run, module.getId())));
        return producedByList;
    }*/

    /**
     * get Artifact build  Produce by data list
     *
     * @param run            - run build details
     * @param sha1           - sha1 checksum
     * @param md5            - md5 checksum
     * @param producedByList - produce by list
     * @return - list of produce by instances
     */
    protected List<ProduceBy> getArtifactProduceBy(BuildRun run, String sha1, String md5,
            List<ProduceBy> producedByList) {
        ArtifactBuildAddon buildAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(
                ArtifactBuildAddon.class);
        Build build = buildService.getBuild(run);
        List<Module> modules = build.getModules();
        if (modules != null) {
            modules.forEach(module ->
                    addBuildProduceBy(run, sha1, md5, buildAddon, producedByList, build, module));
        }
        return producedByList;
    }

    /**
     * add build produce by data
     *
     * @param run            - build data
     * @param sha1           - sha1 checksum
     * @param md5            - md5 checksum
     * @param buildAddon     - build add on
     * @param producedByList - list of produce by instance
     * @param build          - build full data
     * @param module         - build module instance
     */
    private void addBuildProduceBy(BuildRun run, String sha1, String md5, ArtifactBuildAddon buildAddon,
            List<ProduceBy> producedByList, Build build, Module module) {
        final String moduleId = module.getId();
        List<Artifact> artifacts = module.getArtifacts();
        if (artifacts != null) {
            Set<RepoPath> artifactInfos = locateNonStrictModuleArtifact(buildAddon, build, module, sha1, md5);
                    if (!artifactInfos.isEmpty()) {
                        producedByList.add(new ProduceBy(run, moduleId));
                    }
            }
        }

    /**
     * Locates the given modules produced artifact
     *
     * @param buildAddon Build addon
     * @param build      The build
     * @param module     Module to extract artifact from
     * @return Repo path of produced artifact if found. Null if not
     */
    private Set<RepoPath> locateNonStrictModuleArtifact(ArtifactBuildAddon buildAddon, Build build,
            Module module, String sha1, String md5) {
        Set<RepoPath> artifactRepoPaths = Sets.newHashSet();
        Set<ArtifactoryBuildArtifact> buildFileArtifacts = buildAddon.getBuildArtifactsFileInfos(build);
        List<Artifact> artifacts = module.getArtifacts();
        if (artifacts != null) {
            artifacts.forEach(artifact -> {
                String artifactSha1 = artifact.getSha1();
                String artifactMd5 = artifact.getMd5();
                if ((StringUtils.isNotBlank(artifactSha1) && artifactSha1.equals(sha1)) ||
                        (StringUtils.isNotBlank(artifactMd5) && (artifactMd5.equals(md5)))) {
                    artifactRepoPaths.addAll(
                            buildFileArtifacts.stream().filter(
                                    buildArtifact -> buildArtifact.getArtifact().equals(
                                            artifact) && buildArtifact.getFileInfo() != null)
                                    .map(buildArtifact -> buildArtifact.getFileInfo().getRepoPath())
                                    .collect(Collectors.toList()));
                }
            });
        }
        return artifactRepoPaths;
    }

    /**
     * get Artifact build used by data List
     *
     * @param run        - run build details
     * @param sha1       - sha1 checksum
     * @param md5        - md5 checksum
     * @param usedByList - produce by list
     * @return - list of produce by instances
     */
    protected List<UsedBy> addBuildUsedBy(BuildRun run, String sha1, String md5, List<UsedBy> usedByList) {
        Build build = buildService.getBuild(run);
        List<Module> modules = build.getModules();
        if (modules != null) {
            modules.forEach(module ->
                    addBuildUsedBy(run, sha1, md5, usedByList, module));
        }
        return usedByList;
    }

    /**
     * add build used by data
     *
     * @param run        - build data
     * @param sha1       - sha1 checksum
     * @param md5        - md5 checksum
     * @param usedByList - list of produce by instance
     * @param module     - build module instance
     */
    private void addBuildUsedBy(BuildRun run, String sha1, String md5, List<UsedBy> usedByList, Module module) {
        List<Dependency> dependencies = module.getDependencies();
        if (dependencies != null) {
            createUsedByModule(module, sha1, md5, usedByList, run);
        }
    }

    /**
     * Locates the given modules produced dependency
     *
     * @param module     Module to extract dependency from
     * @return Repo path of produced dependency if found. Null if not
     */
    private Set<RepoPath> createUsedByModule(Module module, String sha1, String md5, List<UsedBy> usedByList,
            BuildRun run) {
        Set<RepoPath> artifactRepoPaths = Sets.newHashSet();
        module.getDependencies().forEach(dependency -> {
            if ((StringUtils.isNotBlank(dependency.getSha1()) && dependency.getSha1().equals(sha1) ||
                    (StringUtils.isNotBlank(dependency.getMd5()) && dependency.getMd5().equals(
                            md5)))) {
                final Set<String> scopes = dependency.getScopes();
                if (CollectionUtils.isNullOrEmpty(scopes)) {
                    usedByList.add(new UsedBy(run, module.getId(), ""));
                } else {
                    for (String scope : scopes) {
                        usedByList.add(new UsedBy(run, module.getId(), scope));
                    }
                }
            }
        });
        return artifactRepoPaths;
    }
}
