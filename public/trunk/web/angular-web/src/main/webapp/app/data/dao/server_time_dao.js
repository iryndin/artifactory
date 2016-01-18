export function ServerTimeDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.SERVER_TIME)
            .setCustomActions({
            	'get': {
                    method: 'GET'
				}
            })
            .getInstance();
}