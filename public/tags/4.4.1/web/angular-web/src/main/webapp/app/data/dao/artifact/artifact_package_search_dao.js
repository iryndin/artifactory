import {ArtifactoryDao} from '../../artifactory_dao';

export function ArtifactPackageSearchDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.ARTIFACT_SEARCH + '/pkg/:param')
            .setCustomActions({
                'availablePackages': {
                    method: 'GET',
                    isArray: true,
                    params: {param: 'availablePackages'}
                },
                'queryFields': {
                    method: 'GET',
                    isArray: true,
                    params: {param: '@packageType'}
                },
                'runQuery': {
                    method: 'POST',
                    notifications: true
                }
            })
            .getInstance();
}