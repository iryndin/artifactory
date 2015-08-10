import {AdminConfigurationLicensesController} from './licenses.controller';
import {AdminConfigurationLicenseFormController} from './license_form.controller'

function licensesConfig($stateProvider) {
    $stateProvider
            .state('admin.configuration.licenses', {
                url: '/licenses',
                templateUrl: 'states/admin/configuration/licenses/licenses.html',
                controller: 'AdminConfigurationLicensesController as AdminConfigurationLicenses'
            })
            .state('admin.configuration.licenses.edit', {
                parent: 'admin.configuration',
                url: '/licenses/{licenseName}/edit',
                templateUrl: 'states/admin/configuration/licenses/license_form.html',
                controller: 'AdminConfigurationLicenseFormController as AdminLicenseForm'
            })
            .state('admin.configuration.licenses.new', {
                parent: 'admin.configuration',
                url: '/licenses/new',
                templateUrl: 'states/admin/configuration/licenses/license_form.html',
                controller: 'AdminConfigurationLicenseFormController as AdminLicenseForm'
            })
}


export default angular.module('configuration.licenses', ['ui.router'])
        .config(licensesConfig)
        .controller('AdminConfigurationLicensesController', AdminConfigurationLicensesController)
        .controller('AdminConfigurationLicenseFormController', AdminConfigurationLicenseFormController);        