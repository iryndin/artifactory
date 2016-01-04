import {AdminAdvancedSupportPageController} from './support_page.controller';


function supportPageConfig($stateProvider) {
    $stateProvider
            .state('admin.advanced.support_page', {
                params: {feature: 'supportPage'},
                url: '/support_page',
                templateUrl: 'states/admin/advanced/support_page/support_page.html',
                controller: 'AdminAdvancedSupportPageController as SupportPage'
            })
}

export default angular.module('advanced.support_page', [])
        .config(supportPageConfig)
        .controller('AdminAdvancedSupportPageController', AdminAdvancedSupportPageController);