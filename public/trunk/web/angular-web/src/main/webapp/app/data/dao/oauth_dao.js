import {ArtifactoryDao} from '../artifactory_dao';

export function OAuthDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.OAUTH + "/:p1/:p2/:p3/:p4")
            .setCustomActions({
                'get':{
                    method: 'GET'
                },
                'update':{
                    method: 'POST',
                    notifications: true
                },
                'createProvider':{
                    method: 'PUT',
                    params: {p1: 'provider'},
                    notifications: true
                },
                'updateProvider':{
                    method: 'POST',
                    params: {p1: 'provider'},
                    notifications: true
                },
                'deleteProvider':{
                    method: 'DELETE',
                    params: {p1: 'provider', p2: '@provider'},
                    notifications: true
                },
                'getUserTokens':{
                    method: 'GET',
                    isArray: true,
                    params: {p1: 'user', p2: 'tokens'}
                },
                'deleteUserToken':{
                    method: 'DELETE',
                    params: {p1: 'user', p2: 'tokens', p3: '@username', p4: '@provider'}
                }
            })
            .getInstance();
}