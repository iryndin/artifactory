function oauthErrorConfig ($stateProvider) {

    $stateProvider
            .state('oauth_error', {
                url: '/oauth_error',
                parent: 'app-layout',
            })
}

export default angular.module('oauth_error', [])
        .config(oauthErrorConfig)
