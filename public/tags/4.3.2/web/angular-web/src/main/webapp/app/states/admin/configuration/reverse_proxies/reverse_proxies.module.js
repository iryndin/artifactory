import {AdminConfigurationReverseProxiesController} from './reverse_proxies.controller';
import {AdminConfigurationReverseProxyFormController} from './reverse_proxy_form.controller';

function reverseProxiesConfig($stateProvider) {
    $stateProvider
/*
            .state('admin.configuration.reverse_proxies', {
                params: {feature: 'ReverseProxies'},
                url: '/reverse_proxies',
                templateUrl: 'states/admin/configuration/reverse_proxies/reverse_proxies.html',
                controller: 'AdminConfigurationReverseProxiesController as AdminConfigurationReverseProxies'
            })
            .state('admin.configuration.reverse_proxies.new', {
                params: {feature: 'ReverseProxies'},
                parent: 'admin.configuration',
                url: '/reverse_proxies/new',
                templateUrl: 'states/admin/configuration/reverse_proxies/reverse_proxy_form.html',
                controller: 'AdminConfigurationReverseProxyFormController as ReverseProxyForm'
            })
            .state('admin.configuration.reverse_proxies.edit', {
                params: {feature: 'ReverseProxies'},
                parent: 'admin.configuration',
                url: '/reverse_proxies/:reverseProxyKey/edit',
                templateUrl: 'states/admin/configuration/reverse_proxies/reverse_proxy_form.html',
                controller: 'AdminConfigurationReverseProxyFormController as ReverseProxyForm'
            })
*/
            .state('admin.configuration.reverse_proxy', {
                params: {feature: 'ReverseProxies', reverseProxyKey: 'nginx'},
                parent: 'admin.configuration',
                url: '/reverse_proxy',
                templateUrl: 'states/admin/configuration/reverse_proxies/reverse_proxy_form.html',
                controller: 'AdminConfigurationReverseProxyFormController as ReverseProxyForm'
            })
}

export default angular.module('configuration.reverse_proxies', [])
        .config(reverseProxiesConfig)
        .controller('AdminConfigurationReverseProxiesController', AdminConfigurationReverseProxiesController)
        .controller('AdminConfigurationReverseProxyFormController', AdminConfigurationReverseProxyFormController);