import {ArtifactoryDao} from '../artifactory_dao';

export function UserProfileDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setDefaults({method: 'POST'})
        .setPath(RESOURCE.USER_PROFILE)
        .setCustomActions({
            fetch: {
                notifications: true
            },
            getApiKey: {
                method: 'GET',
                path: RESOURCE.USER_API_KEY + '/:user',
                params: {user: '@username'}
            },
            getAndCreateApiKey: {
                method: 'POST',
                path: RESOURCE.USER_API_KEY
            },
            regenerateApiKey: {
                method: 'PUT',
                path: RESOURCE.USER_API_KEY
            },
            revokeApiKey: {
                method: 'DELETE',
                path: RESOURCE.USER_API_KEY + '/:user',
                params: {user: '@username'},
                notifications: true
            }
        }).getInstance();
}
