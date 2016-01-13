export class AdminSecurityGeneralDao {
    constructor(ArtifactoryDaoFactory, RESOURCE) {
	    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.SECURITY_CONFIG)
            .setCustomActions({
                'unlockUsers': {
                    path: RESOURCE.SECURITY_CONFIG + '/unlockUsers',
                    method: 'POST',
                    notifications: true
                },
                'unlockAllUsers': {
                    path: RESOURCE.SECURITY_CONFIG + '/unlockAllUsers',
                    method: 'POST',
                    notifications: true
                }
            })
            .getInstance();
    }
}