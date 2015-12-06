import {AdminSecurityOAuthController} from './oauth.controller';
import {AdminSecurityOAuthProviderFormController} from './oauth_provider_form.controller';

function oauthConfig($stateProvider) {

    $stateProvider
            .state('admin.security.oauth', {
                params: {feature: 'oauthsso'},
                url: '/oauth',
                templateUrl: 'states/admin/security/oauth/oauth.html',
                controller: 'AdminSecurityOAuthController as AdminSecurityOAuth'
            })
            .state('admin.security.oauth.edit', {
                params: {feature: 'oauthsso'},
                parent: 'admin.security',
                url: '/oauth/{providerName}/edit',
                templateUrl: 'states/admin/security/oauth/oauth_provider_form.html',
                controller: 'AdminSecurityOAuthProviderFormController as ProviderForm'
            })
            .state('admin.security.oauth.new', {
                params: {feature: 'oauthsso'},
                parent: 'admin.security',
                url: '/oauth/newprovider',
                templateUrl: 'states/admin/security/oauth/oauth_provider_form.html',
                controller: 'AdminSecurityOAuthProviderFormController as ProviderForm'
            })

}

export default angular.module('security.oauth', [])
        .config(oauthConfig)
        .controller('AdminSecurityOAuthController', AdminSecurityOAuthController)
        .controller('AdminSecurityOAuthProviderFormController', AdminSecurityOAuthProviderFormController);