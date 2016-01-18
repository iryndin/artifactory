import {ArtifactoryDao} from '../artifactory_dao';

export function SshClientDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
        .setDefaults({method: 'POST'})
        .setPath(RESOURCE.SSH_CLIENT)
        .setCustomActions({
            fetch: {
                notifications: true
            }
        }).getInstance();
}
