package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.actions.deleteversions;

import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.DeleteArtifactVersions;
import org.artifactory.ui.rest.model.common.RepoKeyPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetDeleteVersionsService implements RestService {

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String repoKey = request.getQueryParamByKey("repoKey");
        String path = request.getQueryParamByKey("path");
        RepoPath repoPath = InternalRepoPathFactory.create(repoKey, path);
        // update delete version module
        Collection<DeleteArtifactVersions> deleteVersionModel = getDeleteVersionModel(repoPath);
        // update response data
        response.iModelList(deleteVersionModel);
    }

    /**
     * search for unit versions and update delete version model data
     *
     * @param repoPath - repo path
     */
    private Collection<DeleteArtifactVersions> getDeleteVersionModel(RepoPath repoPath) {
        ItemSearchResults<VersionUnitSearchResult> results = repositoryService.getVersionUnitsUnder(repoPath);
        Map<String, Integer> moduleCountMap = new HashMap<>();
        Map<String, DeleteArtifactVersions> moduleArtifactVersionMap = new HashMap<>();
        // filter version unit with same module id
        results.getResults().forEach(result ->
                populateUnitVersionResult(moduleCountMap, moduleArtifactVersionMap, result));
        // update directories count
        moduleCountMap.keySet().forEach(module -> {
            moduleArtifactVersionMap.get(module).setDirectoriesCount(moduleCountMap.get(module));
        });
        return moduleArtifactVersionMap.values();
    }

    /**
     * populate unit version results to delete version module
     *
     * @param moduleCountMap           - hole models counter
     * @param moduleArtifactVersionMap - hold unnit version info by module
     * @param result                   - unit version results
     */
    private void populateUnitVersionResult(Map<String, Integer> moduleCountMap,
            Map<String, DeleteArtifactVersions> moduleArtifactVersionMap, VersionUnitSearchResult result) {
        String groupKeyVersion = toGroupVersionKey(result.getVersionUnit().getModuleInfo());
        if (moduleCountMap.get(groupKeyVersion) == null) {
            moduleCountMap.put(groupKeyVersion, 1);
            moduleArtifactVersionMap.put(groupKeyVersion, new DeleteArtifactVersions(result.getVersionUnit()));
        } else {
            Integer moduleCount = moduleCountMap.get(groupKeyVersion);
            result.getVersionUnit().getRepoPaths().forEach(repoPath ->
                    moduleArtifactVersionMap.get(groupKeyVersion).getRepoPaths().add(
                            new RepoKeyPath(repoPath.getPath(), repoPath.getRepoKey())));
            moduleCountMap.put(groupKeyVersion, moduleCount + 1);
        }
    }

    /**
     * generate group version key by module info
     *
     * @param info - module info
     * @return group version key
     */
    private String toGroupVersionKey(ModuleInfo info) {
        StringBuilder groupVersionKeyBuilder =
                new StringBuilder(info.getOrganization()).append(":").append(info.getBaseRevision());
        if (info.isIntegration()) {

            groupVersionKeyBuilder.append("-");
            if (MavenNaming.SNAPSHOT.equals(info.getFolderIntegrationRevision())) {
                groupVersionKeyBuilder.append(MavenNaming.SNAPSHOT);
            } else {
                groupVersionKeyBuilder.append("INTEGRATION");
            }
        }
        return groupVersionKeyBuilder.toString();
    }
}
