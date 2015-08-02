import {ArtifactoryDao} from '../../artifactory_dao';

export function ArtifactDeployDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setDefaults({method: 'POST'})
            .setPath(RESOURCE.ARTIFACT + '/:action')
            .setCustomActions({
                'post': {
                    method: 'POST',
                    params: {
                        action: '@action'
                    },
                    notifications: true
                }
            })
            .getInstance();
}