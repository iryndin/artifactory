export function SupportPageDao(RESOURCE, ArtifactoryDaoFactory) {
    return ArtifactoryDaoFactory()
            .setPath(RESOURCE.SUPPORT_PAGE + '/:action/:filename')
            .setCustomActions({
                'generateBundle': {
                    method: 'POST',
                    isArray: true,
                    params: {action: 'generateBundle'}
                },
                'listBundles': {
                    method: 'GET',
                    isArray: true,
                    params: {action: 'listBundles'}
                },
                'deleteBundle': {
                    method: 'DELETE',
                    params: {action: 'deleteBundle', filename: '@filename'}
                }
            })
            .getInstance();
}