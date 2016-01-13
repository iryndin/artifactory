export function ReverseProxiesDao(RESOURCE, ArtifactoryDaoFactory) {
	return ArtifactoryDaoFactory()
    	.setPath(RESOURCE.REVERSE_PROXIES + "/:prefix/:key")
        .setCustomActions({
            'delete': {
                method : 'POST',
                params: {prefix: 'deleteReverseProxies'}
            },
            'update': {
                method : 'PUT',
                params :{prefix: 'crud', key :'@key'}
            },
            'get': {
                method : 'GET',
                params :{prefix: 'crud', key :'@key'}
            },
            'checkPort': {
                    method: 'GET',
                    params: {prefix: 'checkPort', key: '@port'}
            }
        })
        .getInstance();
}
