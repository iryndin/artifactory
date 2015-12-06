import {ArtifactoryDao} from '../../artifactory_dao';

export function ArtifactViewsDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setDefaults({method: 'POST'})
            .setPath(RESOURCE.VIEWS+"/:view")
            .setCustomActions({
                'fetch':{
                    method: 'POST',
                    params:{view: '@view'}
                },
                'getDockerProxySnippet': {
                    method: 'GET',
                    path: RESOURCE.VIEWS + "/dockerproxy/:repoKey",
                    params:{repoKey: '@repoKey'}
                }
            })
            .getInstance();
}
